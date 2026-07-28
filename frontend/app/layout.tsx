import type { Metadata } from "next";
import { Cormorant_Garamond, Source_Sans_3 } from "next/font/google";
import "./globals.css";

const display = Cormorant_Garamond({ subsets: ["latin"], variable: "--font-display", weight: ["500", "600", "700"] });
const interfaceFont = Source_Sans_3({ subsets: ["latin"], variable: "--font-interface", weight: ["400", "500", "600"] });

export const metadata: Metadata = {
  title: process.env.NEXT_PUBLIC_APP_NAME ?? "Narrative Platform",
  description: "Narrative tools for tabletop RPG campaigns.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR">
      <body className={`${display.variable} ${interfaceFont.variable}`}>{children}</body>
    </html>
  );
}
