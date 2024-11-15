FROM node:20.16.0
WORKDIR /app
COPY EcoTraceServer/package*.json ./EcoTraceServer/
RUN cd EcoTraceServer && npm install
COPY . .
WORKDIR /app/EcoTraceServer
EXPOSE 35288
CMD ["node", "server.js"]