FROM node:20-alpine AS build
WORKDIR /app
COPY gift-chat-uni/package*.json ./
RUN npm ci
COPY gift-chat-uni/ ./
ARG VITE_API_BASE_URL=/api
ARG VITE_WS_BASE_URL=/ws
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_BASE_URL=$VITE_WS_BASE_URL
RUN npm run build:h5

FROM nginx:1.27-alpine
COPY --from=build /app/dist/build/h5 /usr/share/nginx/html
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
