# Etapa 1: compila el frontend React
FROM node:20-alpine AS build
WORKDIR /app
COPY frontend/package.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Etapa 2: nginx sirve el frontend y hace de API Gateway
FROM nginx:1.27-alpine
COPY gateway/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
