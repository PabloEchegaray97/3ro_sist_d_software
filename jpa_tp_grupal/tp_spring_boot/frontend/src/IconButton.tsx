import type { ButtonHTMLAttributes } from "react";

interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  icon: string;
  label: string;
}

export function IconButton({ icon, label, className = "", ...props }: IconButtonProps) {
  return (
    <button
      {...props}
      className={`icon-button ${className}`.trim()}
      aria-label={label}
      title={label}
    >
      <i className={`ph ${icon}`} aria-hidden="true" />
    </button>
  );
}
