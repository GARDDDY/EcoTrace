FROM node:22-alpine
WORKDIR /app
COPY EcoTraceServer/package*.json ./
RUN npm ci --omit=dev
COPY EcoTraceServer/ ./
EXPOSE 35288
CMD ["node", "server.js"]
