# JSON Database (Kotlin)

## Overview
This project is a simple JSON-based database implemented in Kotlin. It supports basic CRUD operations—**GET**, **SET**, and **DELETE**—over a JSON structure. The database operates on key-value pairs and supports nested structures, allowing you to store and manipulate complex JSON data.

## Features
- **GET**: Retrieve a value from the database using a specified key path.
- **SET**: Insert or update a value in the database at a specified key path.
- **DELETE**: Remove a value from the database at a specified key path.
- **Thread-Safety**: The database operations are protected by read-write locks to ensure consistency in a multi-threaded environment.

## Project Structure
- **Client**: Handles sending requests to the server using a simple command-line interface or by reading input from files.
- **Server**: Manages incoming client requests, performs the requested operations on the database, and returns the appropriate responses.
- **Model**: Defines the data models (`Request` and `Response`) used for communication between the client and server.

## How It Works
1. The **client** sends a request to the **server** via TCP connection.
2. The **server** processes the request, performs the required operation (**GET**, **SET**, **DELETE**), and sends back a response.
3. The **server** maintains the database in a JSON file, ensuring that changes are persistent across sessions.
