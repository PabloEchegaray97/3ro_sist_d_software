const sentenceCase = (value: string) => {
  const normalized = value.trim().toLocaleLowerCase("es-AR");
  return normalized ? normalized[0].toLocaleUpperCase("es-AR") + normalized.slice(1) : "";
};

export type PriceListTier = "retail" | "wholesale" | "other";

export function priceListTier(code: string, denomination = ""): PriceListTier {
  const value = `${code} ${denomination}`.toLocaleLowerCase("es-AR");
  if (value.includes("minorista")) return "retail";
  if (value.includes("mayorista")) return "wholesale";
  return "other";
}

export function shortPriceListLabel(code: string, denomination = ""): string {
  const tier = priceListTier(code, denomination);
  if (tier === "retail") return "Minorista";
  if (tier === "wholesale") return "Mayorista";
  return sentenceCase((denomination || code).replace(/^lista\s+/i, ""));
}

export function fullPriceListLabel(code: string, denomination: string): string {
  const shortLabel = shortPriceListLabel(code, denomination);
  return `Lista ${shortLabel.toLocaleLowerCase("es-AR")}`;
}
