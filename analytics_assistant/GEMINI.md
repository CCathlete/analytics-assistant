- This is the backend part of an analytics assisstant application.
- We work with monadic flow, explicit pattern matching (switch better than map and bind), async io and dependency injection in Java.
- Dependencies are handled with Maven.
- This app's purpose is:
    1. Read a config json, taking uels from it. Those urls point to data from github about commits and merges statistics.
    2. Send the data to an endpoint of embedding files to a knowledge base in open webui.
    3. Recieve a prompt from the front end, validate it (malware, denial of service etc), and pass it to a chat endpoint
    for a model in openwebui.
    4. The model needs to prepare data from the knowledge base according to the prompt in a format meant to be used for visualisation.
    5. The Java backend then recieves the data from openwebui's response and format it in a way Apache Superset could generate charts out of.
    6. The backend then sends the data to Apache Superset to create charts from, gets the charts and passes them to the front end to embed in iframes.
- The authentication is done as follows:
    1. The frontend shows an authentication page for username and password.
    2. The backend gets the credentials and passes them to openwebui.
    3. Openwebui authenticates the credentials.
    4. The backend gets a response from openwebui that holds information whether the auth was successful or not (the user needs to have access to the model being used).
    5. That way, openwebui handles the secure authentication and the backend acts as a relay.

- The overall purpose is for this app to provide a prompt base data analysis using openwebui, Apache Superset, React (vite) and Jave (spring).

