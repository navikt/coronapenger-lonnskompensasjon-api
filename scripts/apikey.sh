#!/usr/bin/env bash

echo "Setting up gateway api key"
export API_KEY=$(cat /secret/apikey/x-nav-apiKey)
