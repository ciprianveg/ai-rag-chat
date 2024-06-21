# ai-rag-chat
Setup:
1) download and install ollama from https://ollama.com/download

set these env params:
	set OLLAMA_NUM_PARALLEL=5
	set OLLAMA_MAX_LOADED_MODELS=3
	execute this command:
		ollama serve
download aya and nomic embed models:
	ollama pull aya:8b-23-q6_K
	ollama pull nomic-embed-text:latest 

2) install java 21
3) clone, build and run project:
clone repository:
	https://github.com/ciprianveg/ai-rag-chat.git
build: 
	mvn clean install
	ai-rag-chat-0.0.1-SNAPSHOT.jar is generated in target/


Settings:
in application.properties:
- it is defined the path to static resources, this should be outside the jar to enable creation of new user pages without jar rebuild:
	spring.web.resources.static-locations=file:C:/ai-rag/static
	static.resources.dir=C:/ai-rag/static
	- here we need to copy the content of the static folder from the git project
- define admin.username and admin.password 
- set model name (after pulling it from ollama)

to run it:
java -jar ai-rag-chat-0.0.1-SNAPSHOT.jar

it will be running at visible at localhost:8080