export const money = (amount: number) => new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
}).format(amount);

export const decimal = (amount: number) => new Intl.NumberFormat("es-AR", {
  maximumFractionDigits: 2,
}).format(amount);

export const roundMoney = (amount: number) => Math.round(amount * 100) / 100;

export const formatStatus = (status: string) =>
  status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();

export const errorMessage = (error: unknown) =>
  error instanceof Error ? error.message : "Ocurrió un error";

export const localDateTime = () => {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
};

export function dateTimeParts(value: string): { date: string; time: string } {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return { date: value, time: "" };
  return {
    date: new Intl.DateTimeFormat("es-AR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    }).format(date),
    time: new Intl.DateTimeFormat("es-AR", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(date),
  };
}
