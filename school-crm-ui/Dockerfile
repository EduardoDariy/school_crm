# Этап 1: сборка приложения в контейнере (Node.js на хосте не нужен)
FROM node:22-alpine AS build
WORKDIR /app

# Зависимости ставятся строго по package-lock.json
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

# Копируем код и собираем production-версию
COPY . .
RUN npm run build

# Этап 2: раздача статики через Nginx
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/school-crm-ui/browser /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
