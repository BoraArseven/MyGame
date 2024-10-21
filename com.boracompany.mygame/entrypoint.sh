#!/bin/bash

echo "Starting entrypoint script..."

# Start Xvfb in the background
Xvfb :99 -screen 0 1024x768x16 &
export DISPLAY=:99

# Wait for PostgreSQL to be available
./wait-for-it.sh postgresdb:5432 --timeout=30 --strict -- \
java -cp /app/app.jar com.boracompany.mygame.main.Main
