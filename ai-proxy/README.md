
## Test Local Model
### Run the following command to test the local model with ollama
`ollama run mistral`
`ollama run llama3`

Test Request with HTTPie
1. Run GatewayApp
2. Run`echo -n 'tell me a joke' |http POST  :8080/chat x-llm:ollama` 

Or Run
`GatewayApplication in LLMProxyTest.java`