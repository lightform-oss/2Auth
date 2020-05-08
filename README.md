# 2Auth
RFC6749 implementation with modular HTTP and persistence layers.

## Overview
```
HTTP Requests  
     ⟱ 
   Server
     ⟱ 
  Endpoints 
     ⟱
  Services
     ⟱
Repositories
```

### Server

The server is responsible for receiving HTTP requests, extracting parameters, 
building models, and invoking either the authorization or token endpoint.

### Endpoints

The endpoints are the core OAuth logic.
They receive abstract requests, use services for extra-standard behavior, 
and return final responses to be returned to clients.

### Services

Services define the interface between standards based behavior of the 
endpoints and implementation specific behavior.  
To make usage easier, some services have been implemented with best-practices 
behaviors, and only require that you provide repositories to back them.

### Repositories

Simple CRUD data layers, repositories are used with services
and very simple to implement for quick integration.
