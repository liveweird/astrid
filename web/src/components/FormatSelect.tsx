import { CheckIcon, Combobox, Group, InputBase, useVirtualizedCombobox } from "@mantine/core";
import { useCallback, useId, useLayoutEffect, useMemo, useRef, useState } from "react";

export interface FormatOption {
  value: string;
  label: string;
}

interface FormatSelectProps {
  data: FormatOption[];
  value: string;
  disabled?: boolean;
  onChange: (value: string) => void;
}

interface EditingState {
  query: string;
  highlightedValue: string | null;
}

const OPTIONS_HEIGHT_VARIABLE = "--astrid-format-options-available-height";

interface DropdownSizeInput {
  availableHeight: number;
  availableWidth: number;
  elements: { floating: HTMLElement };
}

export default function FormatSelect(props: FormatSelectProps) {
  return <FormatSelectControl {...props} />;
}

function FormatSelectControl({ data, value, disabled = false, onChange }: FormatSelectProps) {
  const [editing, setEditing] = useState<EditingState | null>(null);
  const [controlled, setControlled] = useState({ value, disabled });
  if (controlled.value !== value || controlled.disabled !== disabled) {
    setControlled({ value, disabled });
    setEditing(null);
  }
  const committed = data.find((option) => option.value === value);
  const committedLabel = committed?.label ?? "";
  const filtered = useMemo(() => {
    if (!editing || editing.query === committedLabel) return data;
    const query = editing.query.toLowerCase();
    return data.filter((option) => option.label.toLowerCase().includes(query));
  }, [committedLabel, data, editing]);
  const selectedOptionIndex = editing
    ? filtered.findIndex((option) => option.value === editing.highlightedValue)
    : -1;
  const committedOptionIndex = filtered.findIndex((option) => option.value === value);
  const idPrefix = useId().replaceAll(":", "");
  const inputId = `${idPrefix}-input`;
  const listId = `${idPrefix}-listbox`;
  const listRef = useRef<HTMLDivElement | null>(null);
  const [listElement, setListElement] = useState<HTMLDivElement | null>(null);
  const sizedDropdownRef = useRef<HTMLElement | null>(null);
  const optionId = useCallback((option: FormatOption | undefined) =>
    option ? `${idPrefix}-format-${encodeURIComponent(option.value)}` : null, [idPrefix]);
  const captureList = useCallback((element: HTMLDivElement | null) => {
    listRef.current = element;
    setListElement(element);
  }, []);

  const clearDropdownSize = useCallback(() => {
    const dropdown = sizedDropdownRef.current;
    if (!dropdown) return;
    dropdown.style.removeProperty("max-width");
    dropdown.style.removeProperty("max-height");
    dropdown.style.removeProperty(OPTIONS_HEIGHT_VARIABLE);
    sizedDropdownRef.current = null;
  }, []);
  const applyDropdownSize = useCallback(({ availableHeight, availableWidth, elements }: DropdownSizeInput) => {
    const dropdown = elements.floating;
    const height = Math.max(0, availableHeight);
    const width = Math.max(0, availableWidth);
    const styles = getComputedStyle(dropdown);
    const verticalChrome = [
      styles.paddingTop,
      styles.paddingBottom,
      styles.borderTopWidth,
      styles.borderBottomWidth,
    ].reduce((total, size) => total + (Number.parseFloat(size) || 0), 0);
    dropdown.style.maxWidth = `${width}px`;
    dropdown.style.maxHeight = `${height}px`;
    dropdown.style.setProperty(OPTIONS_HEIGHT_VARIABLE, `${Math.max(0, height - verticalChrome)}px`);
    sizedDropdownRef.current = dropdown;
  }, []);
  const close = useCallback(() => {
    clearDropdownSize();
    setEditing(null);
  }, [clearDropdownSize]);
  const submit = useCallback((option: FormatOption | undefined) => {
    if (disabled || !option || !data.some((candidate) => candidate.value === option.value)) return;
    close();
    if (option.value !== value) onChange(option.value);
  }, [close, data, disabled, onChange, value]);
  const setSelectedOptionIndex = useCallback((index: number) => {
    const option = filtered[index];
    setEditing((current) => current ? { ...current, highlightedValue: option?.value ?? null } : current);
  }, [filtered]);

  // This public store is used for controlled synchronous indices, not for virtualized rendering.
  const store = useVirtualizedCombobox({
    opened: editing !== null,
    onOpenedChange: (opened) => {
      if (!opened) close();
      else if (!disabled) setEditing({ query: committedLabel, highlightedValue: committed?.value ?? data[0]?.value ?? null });
    },
    totalOptionsCount: filtered.length,
    selectedOptionIndex,
    setSelectedOptionIndex,
    activeOptionIndex: committedOptionIndex >= 0 ? committedOptionIndex : 0,
    getOptionId: (index) => optionId(filtered[index]),
    onSelectedOptionSubmit: (index) => submit(filtered[index]),
  });

  const highlightedId = editing
    ? optionId(filtered.find((option) => option.value === editing.highlightedValue)) ?? undefined
    : undefined;
  const keepHighlightVisible = useCallback(() => {
    const list = listRef.current;
    const highlighted = highlightedId ? document.getElementById(highlightedId) : null;
    if (!list || !highlighted || !list.contains(highlighted)) return;
    const listBounds = list.getBoundingClientRect();
    const optionBounds = highlighted.getBoundingClientRect();
    const visibleTop = listBounds.top + list.clientTop;
    const visibleBottom = visibleTop + list.clientHeight;

    // Oversized labels always align to the start, avoiding top/bottom correction oscillation.
    if (optionBounds.height > list.clientHeight || optionBounds.top < visibleTop) {
      list.scrollTop += optionBounds.top - visibleTop;
    } else if (optionBounds.bottom > visibleBottom) {
      list.scrollTop += optionBounds.bottom - visibleBottom;
    }
  }, [highlightedId]);
  useLayoutEffect(() => {
    const highlighted = highlightedId ? document.getElementById(highlightedId) : null;
    if (!editing || !listElement || !highlighted || !listElement.contains(highlighted)) return;
    keepHighlightVisible();
    if (typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(keepHighlightVisible);
    observer.observe(listElement);
    observer.observe(highlighted);
    return () => observer.disconnect();
  }, [editing, highlightedId, keepHighlightVisible, listElement]);
  const updateQuery = (query: string) => {
    const normalized = query.toLowerCase();
    const matches = data.filter((option) => option.label.toLowerCase().includes(normalized));
    setEditing({ query, highlightedValue: matches[0]?.value ?? null });
  };

  return (
    <Combobox
      store={store}
      keepMounted={false}
      width="max-content"
      preventPositionChangeWhenVisible={false}
      styles={{
        dropdown: { padding: "min(0.25rem, 4px)" },
        option: {
          paddingBlock: "min(0.375rem, 6px)",
          paddingInline: "min(0.625rem, 10px)",
          lineHeight: 1.4,
        },
      }}
      middlewares={{
        flip: { padding: 10, fallbackStrategy: "bestFit" },
        shift: { padding: 10, mainAxis: true },
        size: { padding: 10, apply: applyDropdownSize },
      }}
      onOptionSubmit={(submitted) => submit(data.find((option) => option.value === submitted))}
    >
      <Combobox.Target
        withExpandedAttribute
        aria-activedescendant={highlightedId}
        aria-controls={editing ? listId : undefined}
        aria-expanded={editing !== null}
      >
        <InputBase
          id={inputId}
          label="Exercise type"
          value={editing?.query ?? committedLabel}
          disabled={disabled}
          rightSection={<Combobox.Chevron />}
          rightSectionPointerEvents="none"
          onFocus={() => store.openDropdown("keyboard")}
          onClick={() => store.openDropdown("mouse")}
          onChange={(event) => updateQuery(event.currentTarget.value)}
          onBlur={close}
        />
      </Combobox.Target>
      <Combobox.Dropdown style={{ maxWidth: "calc(100vw - 20px)" }}>
        <Combobox.Options
          id={listId}
          ref={captureList}
          labelledBy={`${inputId}-label`}
          style={{ maxHeight: `min(220px, var(${OPTIONS_HEIGHT_VARIABLE}, 220px))`, overflowY: "auto" }}
        >
          {filtered.map((option) => (
            <Combobox.Option
              id={optionId(option) ?? undefined}
              key={option.value}
              value={option.value}
              selected={editing?.highlightedValue === option.value}
              active={option.value === value}
              aria-selected={option.value === value}
            >
              <Group gap="xs" wrap="nowrap">
                {option.value === value && <CheckIcon width="0.8em" height="0.8em" />}
                <span>{option.label}</span>
              </Group>
            </Combobox.Option>
          ))}
          {filtered.length === 0 && <Combobox.Empty>No exercise types found</Combobox.Empty>}
        </Combobox.Options>
      </Combobox.Dropdown>
    </Combobox>
  );
}
