#!/usr/bin/env python
"""
One-command demo:

    python run_vitessce_e_geod_155543.py

Steps:
 1. Unzip E-GEOD-155543-normalised-files.zip
 2. Build AnnData from MatrixMarket + genes + barcodes
 3. Merge clusters from E-GEOD-155543.clusters.tsv
 4. Compute t-SNE (perplexity=25)
 5. Export Vitessce files + config to ./vitessce_out
 6. Start a CORS-enabled HTTP server on port 3000
 7. Open Vitessce in browser with a 2x scatter + gene list layout
"""

import os
import sys
import zipfile
import gzip
import io
import json
import threading
import http.server
import socketserver
from urllib.parse import quote_plus

import numpy as np
import pandas as pd
from scipy.io import mmread

import anndata as ad
import scanpy as sc

from vitessce import (
    VitessceConfig,
    Component as cm,
    CoordinationType as ct,
    AnnDataWrapper,
)
from vitessce.data_utils import optimize_adata, VAR_CHUNK_SIZE

EXPERIMENT_ID = "E-GEOD-155543"
NORMALISED_ZIP = f"{EXPERIMENT_ID}-normalised-files.zip"
CLUSTERS_TSV = f"{EXPERIMENT_ID}.clusters.tsv"
OUT_DIR = "vitessce_out"
PORT = 3000


# ---------- Helpers for file discovery ----------

def ensure_unzipped_normalised():
    if not os.path.exists(NORMALISED_ZIP):
        print(f"ERROR: {NORMALISED_ZIP} not found in current directory.")
        sys.exit(1)

    extract_dir = os.path.join("normalised_files")
    if not os.path.isdir(extract_dir):
        print(f"Unzipping {NORMALISED_ZIP} -> {extract_dir} ...")
        os.makedirs(extract_dir, exist_ok=True)
        with zipfile.ZipFile(NORMALISED_ZIP) as zf:
            zf.extractall(extract_dir)
    else:
        print(f"Using already-unzipped normalised files in {extract_dir}")

    return extract_dir


def find_matrix_genes_barcodes(extract_dir):
    """
    Find:
      - MatrixMarket file (.mtx or .mtx.gz)
      - genes/features file
      - barcodes/cells file

    First, handle the known SCEA naming pattern for E-GEOD-155543:
      E-GEOD-155543.aggregated_filtered_normalised_counts.mtx
      E-GEOD-155543.aggregated_filtered_normalised_counts.mtx_rows
      E-GEOD-155543.aggregated_filtered_normalised_counts.mtx_cols

    If those are not present, fall back to the previous heuristic logic.
    """
    # --- 1) Try the explicit known filenames for this experiment ---
    base_mtx = os.path.join(
        extract_dir,
        f"{EXPERIMENT_ID}.aggregated_filtered_normalised_counts.mtx"
    )
    rows_file = base_mtx + "_rows"
    cols_file = base_mtx + "_cols"

    if os.path.exists(base_mtx) and os.path.exists(rows_file) and os.path.exists(cols_file):
        print("Using known SCEA naming pattern:")
        print("  matrix :", base_mtx)
        print("  genes  :", rows_file)
        print("  barcodes/cells:", cols_file)
        return base_mtx, rows_file, cols_file

    # --- 2) Fallback: original heuristic logic (unchanged) ---
    mtx_path = None
    genes_path = None
    barcodes_path = None

    for root, _, files in os.walk(extract_dir):
        for fname in files:
            lower = fname.lower()
            full = os.path.join(root, fname)
            if lower.endswith(".mtx") or lower.endswith(".mtx.gz"):
                mtx_path = full
            elif lower.endswith(".tsv") or lower.endswith(".txt"):
                if "gene" in lower or "feature" in lower:
                    genes_path = full
                elif "barcode" in lower or "cell" in lower or "assay" in lower:
                    barcodes_path = full

    if not mtx_path or not genes_path or not barcodes_path:
        print("ERROR: Could not locate matrix / genes / barcodes TSVs in "
              f"{extract_dir}.")
        print("       Please check the file names and update the script if needed.")
        sys.exit(1)

    print("Found (fallback heuristic):")
    print("  matrix :", mtx_path)
    print("  genes  :", genes_path)
    print("  barcodes/cells:", barcodes_path)
    return mtx_path, genes_path, barcodes_path



# ---------- Build AnnData ----------

def load_matrix(mtx_path):
    print("Reading expression matrix ...")
    if mtx_path.endswith(".gz"):
        with gzip.open(mtx_path, "rb") as f:
            data = mmread(io.BytesIO(f.read()))
    else:
        data = mmread(mtx_path)
    return data.tocsr()


def build_anndata(mtx_path, genes_path, barcodes_path, clusters_path):
    X = load_matrix(mtx_path)

    print("Reading genes ...")
    # assume single column of gene IDs
    var = pd.read_csv(genes_path, sep="\t", header=None)
    if var.shape[1] == 1:
        var.columns = ["gene_id"]
    else:
        # SCXA sometimes has gene_id, gene_name etc.
        var.columns = [f"col_{i}" for i in range(var.shape[1])]
    var.index = var.iloc[:, 0].astype(str)

    print("Reading barcodes/cell IDs ...")
    obs = pd.read_csv(barcodes_path, sep="\t", header=None)
    obs.columns = ["cell_id"]
    obs.index = obs["cell_id"].astype(str)

    # ---- NEW ORIENTATION CHECK (minimal logic change) ----
    # X is from MatrixMarket; it can be genes x cells or cells x genes.
    # AnnData expects X: n_obs x n_vars = cells x genes.
    print(f"Matrix shape from file: {X.shape[0]} x {X.shape[1]}")
    print(f"Genes (var) count      : {var.shape[0]}")
    print(f"Cells (obs) count      : {obs.shape[0]}")

    if X.shape[0] == obs.shape[0] and X.shape[1] == var.shape[0]:
        # rows already match cells, cols match genes
        print("Matrix orientation: rows = cells, cols = genes (no transpose).")
    elif X.shape[0] == var.shape[0] and X.shape[1] == obs.shape[0]:
        # rows = genes, cols = cells -> transpose
        print("Matrix orientation: rows = genes, cols = cells.")
        print("Transposing matrix so that rows = cells, cols = genes ...")
        X = X.T
        print(f"New matrix shape: {X.shape[0]} x {X.shape[1]}")
    else:
        print("WARNING: Matrix dimensions do not align cleanly with genes/cells.")
        print("  X.shape =", X.shape)
        print("  var (genes) =", var.shape[0])
        print("  obs (cells) =", obs.shape[0])
        print("Proceeding, but results may be incorrect.")

    # ------------------------------------------------------

    adata = ad.AnnData(X=X, obs=obs, var=var)

    # Merge clusters (unchanged logic)
    if not os.path.exists(clusters_path):
        print(f"WARNING: {clusters_path} not found; proceeding without clusters.")
        return adata, None

    print("Reading clusters ...")
    clusters_df = pd.read_csv(clusters_path, sep="\t")

    # Auto-detect cell ID column by maximum overlap with obs.index
    obs_ids = set(adata.obs.index.astype(str))
    best_col = None
    best_overlap = -1
    for col in clusters_df.columns:
        vals = set(clusters_df[col].astype(str))
        overlap = len(obs_ids & vals)
        if overlap > best_overlap:
            best_overlap = overlap
            best_col = col

    if best_overlap <= 0:
        print("WARNING: Could not match any cluster file column to cell IDs.")
        print("         Clusters will be ignored.")
        return adata, None

    cell_id_col = best_col
    print(f"  Detected cell-id column in clusters TSV: {cell_id_col}")

    # Choose cluster column: prefer k_* / *cluster*
    import re
    cluster_cols = []
    for col in clusters_df.columns:
        if col == cell_id_col:
            continue
        if re.search(r"k[_=]\s*\d+", col) or "cluster" in col.lower():
            cluster_cols.append(col)

    if cluster_cols:
        def score(col):
            m = re.search(r"(\d+)", col)
            return int(m.group(1)) if m else 0
        cluster_col = sorted(cluster_cols, key=score)[-1]
    else:
        # fallback to second column
        cluster_col = [c for c in clusters_df.columns if c != cell_id_col][0]

    print(f"  Using cluster column: {cluster_col}")

    clusters_df[cell_id_col] = clusters_df[cell_id_col].astype(str)
    clusters_df = clusters_df.set_index(cell_id_col)
    adata.obs[cluster_col] = clusters_df[cluster_col].reindex(adata.obs.index)

    return adata, cluster_col



def compute_tsne(adata, perplexity=25.0):
    print("Computing PCA + t-SNE (this may take a bit) ...")
    # basic preprocessing for a reasonable embedding
    sc.pp.normalize_total(adata, target_sum=1e4)
    sc.pp.log1p(adata)
    sc.pp.highly_variable_genes(adata, n_top_genes=2000, subset=True)
    sc.pp.scale(adata, max_value=10)
    sc.tl.pca(adata, svd_solver="arpack")
    sc.tl.tsne(adata, use_rep="X_pca", perplexity=perplexity, n_pcs=50)
    adata.obsm["X_tsne"] = adata.obsm["X_tsne"]  # ensure key name
    print("t-SNE complete.")
    return adata


# ---------- Vitessce export ----------

def prepare_vitessce_export(adata, cluster_col):
    print(f"Optimizing AnnData for Vitessce and writing Zarr ...")
    os.makedirs("data_zarr", exist_ok=True)
    zarr_path = os.path.join("data_zarr", f"{EXPERIMENT_ID}.zarr")

    # keep existing optimisation logic
    adata_opt = optimize_adata(
        adata,
        obs_cols=[cluster_col] if cluster_col else [],
        obsm_keys=["X_tsne"],
        var_cols=[],          # you can restrict to hvgs/markers if you like
        optimize_X=True,
    )
    adata_opt.write_zarr(zarr_path, chunks=[adata_opt.shape[0], VAR_CHUNK_SIZE])

    print("Building Vitessce config ...")
    vc = VitessceConfig(
        schema_version="1.0.18",
        name=f"{EXPERIMENT_ID} Vitessce demo",
        description="SCEA-like t-SNE and gene expression views",
    )

    # Dataset + AnnData wrapper
    dataset = vc.add_dataset(name=EXPERIMENT_ID).add_object(
        AnnDataWrapper(
            adata_path=zarr_path,
            obs_embedding_paths=["obsm/X_tsne"],
            obs_embedding_names=["t-SNE"],
            obs_set_paths=[f"obs/{cluster_col}"] if cluster_col else [],
            obs_set_names=[cluster_col] if cluster_col else [],
            obs_feature_matrix_path="X",
        )
    )

    # Two scatterplots using the *embedding name* "t-SNE"
    scatter_clusters = vc.add_view(cm.SCATTERPLOT, dataset=dataset, mapping="t-SNE")
    scatter_gene = vc.add_view(cm.SCATTERPLOT, dataset=dataset, mapping="t-SNE")
    genes = vc.add_view(cm.FEATURE_LIST, dataset=dataset)

    # Only create Cell Sets view if we really have a cluster column
    if cluster_col:
        cell_sets = vc.add_view(cm.OBS_SETS, dataset=dataset)
        # Layout: cell sets + gene list on top, two scatters below
        vc.layout((cell_sets | genes) / (scatter_clusters | scatter_gene))
    else:
        # Layout without cell sets (avoids TypeError when there are none)
        vc.layout(genes / (scatter_clusters | scatter_gene))

    # Coordinate feature selection between right scatter and gene list
    vc.link_views(
        [scatter_gene, genes],
        [ct.FEATURE_SELECTION],
    )

    config_dict = vc.export(
        to="files",
        base_url=f"http://localhost:{PORT}",
        out_dir=OUT_DIR,
    )
    return config_dict



# ---------- CORS-enabled HTTP server ----------

class CORSRequestHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        return super().end_headers()


def start_server(directory, port):
    os.chdir(directory)
    handler = CORSRequestHandler
    with socketserver.TCPServer(("", port), handler) as httpd:
        print(f"Serving {directory} at http://localhost:{port} (Ctrl+C to stop)")
        httpd.serve_forever()


# ---------- Main ----------

def main():
    extract_dir = ensure_unzipped_normalised()
    mtx_path, genes_path, barcodes_path = find_matrix_genes_barcodes(extract_dir)

    adata, cluster_col = build_anndata(
        mtx_path, genes_path, barcodes_path, CLUSTERS_TSV
    )
    adata = compute_tsne(adata, perplexity=25.0)
    config_dict = prepare_vitessce_export(adata, cluster_col)

    # Start server in background thread
    os.makedirs(OUT_DIR, exist_ok=True)
    server_thread = threading.Thread(
        target=start_server, args=(OUT_DIR, PORT), daemon=True
    )
    server_thread.start()

    # Build vitessce.io URL that inlines the config and fetches data from our local server
    vitessce_url = (
        "https://vitessce.io/?url=data:," + quote_plus(json.dumps(config_dict))
    )
    print("\n----------------------------------------------------")
    print("Vitessce is ready.")
    print(f"Open this URL in your browser:\n\n  {vitessce_url}\n")
    print("Keep this Python process running while you explore the data.")
    print("Ctrl+C here when you’re done.")
    print("----------------------------------------------------")

    # Optional: try to open browser automatically
    try:
        import webbrowser
        webbrowser.open(vitessce_url)
    except Exception:
        pass

    # Keep main thread alive while server runs
    try:
        while True:
            pass
    except KeyboardInterrupt:
        print("\nShutting down.")


if __name__ == "__main__":
    main()
