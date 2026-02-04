<img width="1919" height="871" alt="Screenshot From 2026-02-04 09-35-11" src="https://github.com/user-attachments/assets/8022e3a7-8b23-493e-9378-3471abce7014" />

# ARIS (🙂) Analytics Assistant

**Automated Real-time Insight System (ARIS)**

ARIS transforms the traditional BI workflow into a seamless, conversational experience. Instead of manual ETL and chart building, users simply describe the insights they need. The system orchestrates the data pipeline—from vector search to SQL transformation—and renders dynamic Apache Superset visualizations in real-time.

## 🚀 The Vision

In many organizations, generating a new chart is a multi-step process requiring data extraction, transformation, and manual work in a BI tool. ARIS was built to automate this entire lifecycle, allowing users to move from "Prompt to Chart" in seconds.

## 🛠 Tech Stack

| Component | Technology | Role |
| --- | --- | --- |
| **Backend** | **Java 21 / Spring Boot** | Orchestrator & Monadic Logic |
| **Frontend** | **React (Vite) + TypeScript** | Modern, Resizable UI Workspace |
| **AI & RAG** | **OpenWebUI / LiteLLM** | Auth, Knowledge Base, & Data Tagging |
| **Data Platform** | **Apache Spark & Iceberg (Project Nessie)** | Data Processing & Metastore |
| **Data Lake** | **Minio** | Layered Storage (Bronze, Silver, Gold) |
| **Visualization** | **Apache Superset** | Dynamic Chart Rendering & Embedding |
| **Warehouse** | **PostgreSQL** | Structured Data Storage |

## 📐 Architecture & Workflow

The system utilizes a modular adapter-based architecture to separate concerns and ensure scalability across the data lakehouse.

### 1. Data Ingestion & Embedding

Data is ingested from source URLs (e.g., GitHub stats) and processed through Spark. It is then embedded into a vector knowledge base within OpenWebUI, making it queryable via RAG (Retrieval-Augmented Generation).

### 2. Intelligent Processing

The Java backend acts as a strict orchestrator:

* **Authentication**: Credentials are passed to OpenWebUI for secure session management.
* **Contextual Querying**: User prompts (e.g., *"Compare commit activity for 2026"*) are sent to the LLM.
* **Structured Output**: The AI leverages the knowledge base to generate context-aware data in CSV format.

### 3. Real-time Visualization

* **Dataset Sync**: The backend receives CSV data and updates the PostgreSQL warehouse.
* **API Handshake**: Java triggers a metadata refresh in Apache Superset via API.
* **Seamless Embedding**: The resulting chart is embedded into the React frontend via a secure, resizable iframe, bypassing Cloudflare header restrictions through custom configuration.

## 💻 UI/UX Features

* **Resizable Workspace**: Split-screen design allowing users to adjust the size of the prompt editor and visualization area.
* **Real-time Feedback**: Execution states and error handling using a functional, monadic approach.
* **Dark Mode IDE Aesthetic**: A high-contrast interface designed for data engineers and analysts.

Infrastructure terraform code (OpenWebUI, LiteLLM, Superset, Postgres, Spark) can be found in:
https://github.com/CCathlete/pipeline_infra
