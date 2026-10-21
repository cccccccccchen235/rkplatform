/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 接口前缀，见 .env.development / .env.production（两环境一致，07 §7.2）。 */
  readonly VITE_API_BASE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
