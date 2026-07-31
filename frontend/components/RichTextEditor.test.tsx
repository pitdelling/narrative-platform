import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { isContentEmpty, RichTextEditor } from "@/components/RichTextEditor";

describe("isContentEmpty", () => {
  it("treats an editor's empty paragraph as empty", () => {
    expect(isContentEmpty("<p></p>")).toBe(true);
  });

  it("treats blank/whitespace-only content as empty", () => {
    expect(isContentEmpty("")).toBe(true);
    expect(isContentEmpty("<p>   </p>")).toBe(true);
  });

  it("treats actual text content as non-empty", () => {
    expect(isContentEmpty("<p>Hello</p>")).toBe(false);
  });
});

describe("RichTextEditor", () => {
  it("renders the initial value and a toolbar when editable", async () => {
    render(<RichTextEditor value="<p>Hello world</p>" onChange={vi.fn()} ariaLabel="Conteúdo" />);
    expect(await screen.findByText("Hello world")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Negrito" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Itálico" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sublinhado" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Riscado" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Lista com marcadores" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Enumeração de números" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Enumeração de letras minúsculas" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Recuar bloco" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Avançar bloco" })).toBeInTheDocument();
    expect(screen.getByLabelText("Cor do texto")).toBeInTheDocument();
    expect(screen.getByLabelText("Cor de fundo")).toBeInTheDocument();
  });

  it("hides the toolbar and disables editing when readOnly", async () => {
    render(<RichTextEditor value="<p>Locked</p>" onChange={vi.fn()} readOnly ariaLabel="Conteúdo" />);
    await screen.findByText("Locked");
    expect(screen.queryByRole("button", { name: "Negrito" })).not.toBeInTheDocument();
  });

  it("calls onChange with updated HTML as the user types", async () => {
    const onChange = vi.fn();
    render(<RichTextEditor value="" onChange={onChange} ariaLabel="Conteúdo" />);
    const editable = await screen.findByLabelText("Conteúdo");
    await userEvent.click(editable);
    await userEvent.type(editable, "Hi");
    await waitFor(() => expect(onChange).toHaveBeenCalled());
    const lastCall = onChange.mock.calls.at(-1)?.[0] as string;
    expect(lastCall).toContain("Hi");
  });
});
