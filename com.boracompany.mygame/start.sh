#!/bin/bash

# Usage: ./start.sh

# Export the DISPLAY variable from the host environment
export DISPLAY=${DISPLAY:-:0}

# Grant access to the X server
xhost +local:docker

# Run docker-compose
docker compose up --build
