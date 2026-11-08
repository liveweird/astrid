import { MantineProvider } from "@mantine/core";
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import FormatSelect, { type FormatOption } from "./FormatSelect";

const formats: FormatOption[] = [
  { value: "E001", label: "Choose the present form" },
  { value: "E002", label: "Supply the indefinite article" },
  { value: "E003", label: "Arrange a subject-first statement" },
];

function renderSelect(value = "E001", onChange = vi.fn(), disabled = false) {
  return render(
    <MantineProvider env="test">
      <FormatSelect data={formats} value={value} disabled={disabled} onChange={onChange} />
    </MantineProvider>,
  );
}

function ControlledSelect({ initial = "E001" }: { initial?: string }) {
  const [value, setValue] = useState(initial);
  return <FormatSelect data={formats} value={value} onChange={setValue} />;
}

function rectangle(top: number, bottom: number): DOMRect {
  return { x: 0, y: top, top, bottom, left: 0, right: 200, width: 200, height: bottom - top, toJSON: () => ({}) };
}

describe("FormatSelect", () => {
  it("synchronously dismisses and reopens the current format", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    renderSelect("E002", onChange);
    const selector = screen.getByRole("combobox", { name: "Exercise type" });

    await user.click(selector);
    const current = screen.getByRole("option", { name: "Supply the indefinite article" });
    const listbox = screen.getByRole("listbox");
    expect(selector).toHaveAttribute("aria-expanded", "true");
    expect(selector).toHaveAttribute("aria-controls", listbox.id);
    expect(screen.getAllByRole("option")).toHaveLength(3);
    expect(document.getElementById(listbox.getAttribute("aria-labelledby")!)).toHaveTextContent("Exercise type");
    expect(current).toHaveAttribute("data-combobox-selected", "true");
    expect(selector).toHaveAttribute("aria-activedescendant", current.id);
    await user.keyboard("{Enter}");
    expect(selector).toHaveAttribute("aria-expanded", "false");
    expect(selector).toHaveValue("Supply the indefinite article");
    expect(onChange).not.toHaveBeenCalled();
    expect(document.querySelector('[role="listbox"]')).not.toBeInTheDocument();

    await user.click(selector);
    await user.tab();
    expect(document.querySelector('[role="listbox"]')).not.toBeInTheDocument();
    await user.click(selector);
    const reopened = screen.getByRole("option", { name: "Supply the indefinite article" });
    expect(reopened).toHaveAttribute("data-combobox-selected", "true");
    expect(selector).toHaveAttribute("aria-activedescendant", reopened.id);
  });

  it("opens from a focused closed input at the committed option", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    renderSelect("E002", onChange);
    const selector = screen.getByRole("combobox", { name: "Exercise type" });
    await user.click(selector);
    await user.keyboard("{Escape}");
    expect(selector).toHaveFocus();
    expect(selector).toHaveAttribute("aria-expanded", "false");

    await user.keyboard("{ArrowDown}");
    const article = screen.getByRole("option", { name: "Supply the indefinite article" });
    expect(article).toHaveAttribute("data-combobox-selected", "true");
    expect(selector).toHaveAttribute("aria-activedescendant", article.id);
    await user.keyboard("{Enter}");
    expect(selector).toHaveAttribute("aria-expanded", "false");
    expect(selector).toHaveValue("Supply the indefinite article");
    expect(onChange).not.toHaveBeenCalled();
  });

  it("keeps arrow navigation, active descendant and submission on one controlled index", async () => {
    const user = userEvent.setup();
    render(<MantineProvider env="test"><ControlledSelect /></MantineProvider>);
    const selector = screen.getByRole("combobox", { name: "Exercise type" });
    await user.click(selector);
    await user.keyboard("{ArrowDown}");
    const article = screen.getByRole("option", { name: "Supply the indefinite article" });
    expect(article).toHaveAttribute("data-combobox-selected", "true");
    expect(selector).toHaveAttribute("aria-activedescendant", article.id);
    await user.keyboard("{ArrowUp}");
    expect(screen.getByRole("option", { name: "Choose the present form" }))
      .toHaveAttribute("data-combobox-selected", "true");

    await user.clear(selector);
    await user.type(selector, "SUBJECT-FIRST");
    const ordering = screen.getByRole("option", { name: "Arrange a subject-first statement" });
    expect(ordering).toHaveAttribute("data-combobox-selected", "true");
    expect(selector).toHaveAttribute("aria-activedescendant", ordering.id);
    await user.keyboard("{Enter}");
    const updatedSelector = screen.getByRole("combobox", { name: "Exercise type" });
    expect(updatedSelector).toHaveValue("Arrange a subject-first statement");
    expect(updatedSelector).toHaveAttribute("aria-expanded", "false");
  });

  it("clears the highlight for an unmatched query and closes with Escape", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    renderSelect("E001", onChange);
    const selector = screen.getByRole("combobox", { name: "Exercise type" });
    await user.click(selector);
    await user.clear(selector);
    await user.type(selector, "not a format");
    expect(screen.getByText("No exercise types found")).toBeVisible();
    expect(selector).not.toHaveAttribute("aria-activedescendant");
    await user.keyboard("{ArrowDown}{Enter}");
    expect(onChange).not.toHaveBeenCalled();
    await user.keyboard("{Escape}");
    expect(selector).toHaveAttribute("aria-expanded", "false");
    expect(selector).toHaveValue("Choose the present form");
  });

  it("keeps the focused input mounted while an enabled external update closes stale editing", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    const view = renderSelect("E001", onChange);
    const selector = screen.getByRole("combobox", { name: "Exercise type" });
    await user.click(selector);
    await user.clear(selector);
    await user.type(selector, "Arrange");

    view.rerender(
      <MantineProvider env="test">
        <FormatSelect data={formats} value="E002" onChange={onChange} />
      </MantineProvider>,
    );
    const updatedSelector = screen.getByRole("combobox", { name: "Exercise type" });
    expect(updatedSelector).toBe(selector);
    expect(updatedSelector).toHaveFocus();
    expect(updatedSelector).toHaveAttribute("aria-expanded", "false");
    expect(updatedSelector).toHaveValue("Supply the indefinite article");
  });

  it("closes stale editing when disabled", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    const view = renderSelect("E001", onChange);
    await user.click(screen.getByRole("combobox", { name: "Exercise type" }));

    view.rerender(
      <MantineProvider env="test">
        <FormatSelect data={formats} value="E001" disabled onChange={onChange} />
      </MantineProvider>,
    );
    const selector = screen.getByRole("combobox", { name: "Exercise type" });
    expect(selector).toBeDisabled();
    expect(selector).toHaveAttribute("aria-expanded", "false");
  });

  it("restores the committed label when the coordinator rejects a change", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    renderSelect("E001", onChange);
    const selector = screen.getByRole("combobox", { name: "Exercise type" });
    await user.click(selector);
    await user.clear(selector);
    await user.type(selector, "Supply the indefinite article");
    await user.keyboard("{Enter}");
    expect(onChange).toHaveBeenCalledOnce();
    expect(onChange).toHaveBeenCalledWith("E002");
    expect(selector).toHaveValue("Choose the present form");
    expect(selector).toHaveAttribute("aria-expanded", "false");
  });

  it("does not submit a composing Enter and supports pointer submission", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    renderSelect("E001", onChange);
    const selector = screen.getByRole("combobox", { name: "Exercise type" });
    await user.click(selector);
    await user.clear(selector);
    await user.type(selector, "Supply");
    fireEvent.keyDown(selector, { key: "Enter", code: "Enter", keyCode: 229, isComposing: true });
    expect(onChange).not.toHaveBeenCalled();
    expect(selector).toHaveAttribute("aria-expanded", "true");

    await user.click(screen.getByRole("option", { name: "Supply the indefinite article" }));
    expect(onChange).toHaveBeenCalledOnce();
    expect(onChange).toHaveBeenCalledWith("E002");
  });

  it("scrolls only the listbox when keyboard highlight moves below its visible area", async () => {
    const longFormats = Array.from({ length: 8 }, (_, index) => ({
      value: `E00${index + 1}`,
      label: `Exercise format ${index + 1}`,
    }));
    let listHeight = 100;
    const observerCallbacks: Array<() => void> = [];
    class ResizeObserverMock {
      private active = true;

      constructor(callback: ResizeObserverCallback) {
        observerCallbacks.push(() => { if (this.active) callback([], this as unknown as ResizeObserver); });
      }

      observe() {}
      unobserve() {}
      disconnect() { this.active = false; }
    }
    vi.stubGlobal("ResizeObserver", ResizeObserverMock);
    const geometry = vi.spyOn(HTMLElement.prototype, "getBoundingClientRect").mockImplementation(function (this: HTMLElement) {
      if (this.getAttribute("role") === "listbox") return rectangle(0, listHeight);
      if (this.getAttribute("role") === "option") {
        const list = this.parentElement!;
        const index = Array.from(list.querySelectorAll('[role="option"]')).indexOf(this);
        const top = index * 40 - list.scrollTop;
        return rectangle(top, top + 40);
      }
      return rectangle(0, 0);
    });
    const clientHeight = vi.spyOn(HTMLElement.prototype, "clientHeight", "get").mockImplementation(function (this: HTMLElement) {
      return this.getAttribute("role") === "listbox" ? listHeight : 0;
    });
    const pageScroll = 17;
    document.documentElement.scrollTop = pageScroll;
    const user = userEvent.setup();
    render(
      <MantineProvider env="test">
        <FormatSelect data={longFormats} value="E001" onChange={vi.fn()} />
      </MantineProvider>,
    );
    const selector = screen.getByRole("combobox", { name: "Exercise type" });
    await user.click(selector);
    await user.keyboard("{ArrowDown}{ArrowDown}{ArrowDown}");
    const listbox = screen.getByRole("listbox");
    const highlighted = screen.getByRole("option", { name: "Exercise format 4" });
    expect(listbox).toHaveStyle({ overflowY: "auto" });
    expect(highlighted).toHaveAttribute("data-combobox-selected", "true");
    expect(listbox.scrollTop).toBeGreaterThan(0);
    expect(document.documentElement.scrollTop).toBe(pageScroll);

    listHeight = 30;
    observerCallbacks.forEach((callback) => callback());
    expect(highlighted.getBoundingClientRect().top).toBe(listbox.getBoundingClientRect().top);
    const alignedScrollTop = listbox.scrollTop;
    listHeight = 120;
    observerCallbacks.forEach((callback) => callback());
    expect(listbox.scrollTop).toBe(alignedScrollTop);
    expect(document.documentElement.scrollTop).toBe(pageScroll);

    geometry.mockRestore();
    clientHeight.mockRestore();
    vi.unstubAllGlobals();
    document.documentElement.scrollTop = 0;
  });
});
