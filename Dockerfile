FROM node:20.16.0
WORKDIR /app
COPY EcoTraceServer/package*.json ./EcoTraceServer/
RUN cd EcoTraceServer && npm install
COPY . .
RUN chmod +x /app/wait-for-it.sh
WORKDIR /app/EcoTraceServer
EXPOSE 35288
CMD ["./wait-for-it.sh", "mysql-db:3306", "--", "node", "server.js"]