# Этап 1: Сборка приложения в контейнере (Node.js не нужен на хосте)
FROM node:18-alpine AS build
WORKDIR /app

# Копируем списки зависимостей
COPY package.json package-lock.json ./

# Устанавливаем зависимости
RUN npm install

# Копируем весь код и собираем продакшен-версию
COPY . .
RUN npm run build --configuration=production

# Этап 2: Раздача статики через Nginx
FROM nginx:alpine

# Копируем конфиг Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Копируем собранные файлы (путь для Angular 17+)
COPY --from=build /app/dist/school-crm-ui/browser /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]