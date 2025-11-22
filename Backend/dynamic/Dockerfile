# Use an official Node.js runtime as a parent image
FROM node:18-slim

# Set the working directory in the container
WORKDIR /app

# Copy package.json and package-lock.json
COPY package*.json ./

# Install dependencies
RUN npm install --omit=dev

# Copy the rest of the application's code
COPY ./src/ .

# Expose the port the app runs on
EXPOSE 5000

# Command to run the application
CMD ["node", "main.js"]