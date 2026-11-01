FROM node:24.20-bookworm-slim AS builder
WORKDIR /app
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
ARG RESQ_BACKEND_ORIGIN=https://resq-api-staging.up.railway.app
ENV RESQ_BACKEND_ORIGIN=$RESQ_BACKEND_ORIGIN NEXT_TELEMETRY_DISABLED=1
RUN npm run build
FROM node:24.20-bookworm-slim
WORKDIR /app
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 HOSTNAME=0.0.0.0 PORT=8080
COPY --from=builder --chown=node:node /app/.next/standalone ./
COPY --from=builder --chown=node:node /app/.next/static ./.next/static
COPY --from=builder --chown=node:node /app/public ./public
USER node
EXPOSE 8080
CMD ["node","server.js"]
