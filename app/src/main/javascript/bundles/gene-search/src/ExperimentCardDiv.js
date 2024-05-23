import styled from 'styled-components'

export const ExperimentIconDiv = styled.div`
  background-color: ${props => props.background};
  color: ${props => props.color};
  border-radius: 50%;
  font-size: 16px;
  height: 20px;
  width: 20px;
  text-align: center;
  vertical-align: middle;
  margin-left: 4px;
  display: inline-block;
`

export const CardContainerDiv = styled.div`
  height: 100%;
  width: 100%;
  display: flex !important;
  flex-wrap: nowrap;
  align-items: center;
  border: #e6e6e6 solid 1px;
  margin-bottom: 0.5rem;
  padding: 1rem;
  &:hover {
    background-color: #eaeaea;
    cursor: pointer;
  }
`

export const SmallIconDiv = styled.div`
  width: 5%;
  text-align: center;
  font-size: 1rem;
  display: inline-flex;
`

export const IconDiv = styled.div`
  width: 15%;
  text-align: center;
  font-size: 3rem;
`

export const MarkerDiv = styled.div`
  width: 15%;
  text-align: center;
`

export const TitleDiv = styled.p`
  width: 40%;
  text-align: center;
  margin-bottom: 0;
`

export const VariableDiv = styled.div`
  width: 20%;
  text-align: center;
`

export const CountDiv = styled.div`
  width: 10%;
  text-align: center;
`
