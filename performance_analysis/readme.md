# Performance Analysis for SCXA Backend

## Data source
Production logs from 02/02/2025

## pre processing

1. copy logs to logs directory
2. quick and dirty cleanup using shell. to be added to python.
For example for 2025-04-03 log.
```bash
find logs -name "*04_03*" | xargs -n1 -I{} \
awk '{
    split($7, arr, "/");

    subdomain = arr[4];

    time_sec = $10 / 1000;
    
    if (time_sec > 0.5) {
        print subdomain, $7, time_sec;
    }
}' {} | grep -v health | grep -v search > ./scxa_over_.5sec.txt

find logs -name "*04_03*" | xargs -n1 -I{} \
grep 'search?' {} | awk '{
    split($7, arr, "/");

    time_sec = $10 / 1000;
    
    if (time_sec > 0.5) {
        print "search", time_sec, $7, substr($7, index($7, "?") + 1);
    }
}' >> ./scxa_over_.5sec.txt




```
 
## installation and running

1. Create a venv

    ```bash
    python -mvenv .venv
    . .venvn/bin/activate
    ```

2. start jupyter
