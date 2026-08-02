# ADR-0026: imagem de DESENVOLVIMENTO — roda o servidor Vite real (com Hot Module Reload via bind
# mount no docker-compose.dev.yml), não um build estático. Uma imagem de produção (Nginx + build
# estático) é um artefato separado, fora do escopo desta ADR.
FROM node:24-alpine
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
