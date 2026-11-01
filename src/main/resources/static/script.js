const api = {
  products: "/api/products",
  alerts: "/api/alerts/low-stock",
  movements: "/api/movements"
};

const state = { products: [], alerts: [], movements: [] };
const feedback = document.querySelector("#feedback");

async function request(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: { "Content-Type": "application/json", ...options.headers }
  });
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(body?.message || `Request failed (${response.status})`);
  }
  return body;
}

function showFeedback(message, kind = "success") {
  feedback.textContent = message;
  feedback.className = `feedback ${kind}`;
  feedback.hidden = false;
}

function hideFeedback() {
  feedback.hidden = true;
}

function setLoading(isLoading) {
  document.querySelector("#refresh-button").disabled = isLoading;
  if (isLoading) showFeedback("Loading the latest inventory data…", "loading");
}

function cell(text, className) {
  const element = document.createElement("td");
  if (className) element.className = className;
  element.textContent = text;
  return element;
}

function emptyRow(tableBody, columnCount, message) {
  const row = document.createElement("tr");
  const empty = cell(message, "empty-state");
  empty.colSpan = columnCount;
  row.append(empty);
  tableBody.replaceChildren(row);
}

function renderProducts() {
  const body = document.querySelector("#products-table");
  const label = document.querySelector("#product-count-label");
  const lowStockIds = new Set(state.alerts.map((alert) => alert.productId));
  label.textContent = `${state.products.length} product${state.products.length === 1 ? "" : "s"}`;
  if (!state.products.length) {
    emptyRow(body, 6, "No products yet. Add a product to get started.");
  } else {
    const rows = state.products.map((product) => {
      const row = document.createElement("tr");
      const nameCell = document.createElement("td");
      const name = document.createElement("span");
      name.className = "product-name";
      name.textContent = product.name;
      const id = document.createElement("span");
      id.className = "product-id";
      id.textContent = `ID ${product.id}`;
      nameCell.append(name, id);
      row.append(nameCell, cell(product.description), cell(product.quantity, "quantity"),
        cell(product.reorderLevel));
      const status = document.createElement("td");
      const badge = document.createElement("span");
      const isLow = lowStockIds.has(product.id);
      badge.className = `badge ${isLow ? "badge-warning" : "badge-ok"}`;
      badge.textContent = isLow ? "Low stock" : "In stock";
      status.append(badge);
      row.append(status);
      const action = document.createElement("td");
      const details = document.createElement("button");
      details.type = "button";
      details.className = "text-button";
      details.textContent = "Details";
      details.addEventListener("click", () => showProductDetails(product.id));
      action.append(details);
      row.append(action);
      return row;
    });
    body.replaceChildren(...rows);
  }

  document.querySelector("#total-products").textContent = state.products.length.toLocaleString();
  document.querySelector("#total-quantity").textContent = state.products
    .reduce((total, product) => total + product.quantity, 0).toLocaleString();
  document.querySelector("#low-stock-count").textContent = state.alerts.length.toLocaleString();
  updateProductSelects();
}

function updateProductSelects() {
  for (const select of [document.querySelector("#stock-form select[name=productId]"),
    document.querySelector("#movement-filter")]) {
    const selected = select.value;
    const firstOption = select.querySelector("option").cloneNode(true);
    const options = state.products.map((product) => {
      const option = document.createElement("option");
      option.value = product.id;
      option.textContent = `${product.name} (#${product.id})`;
      return option;
    });
    select.replaceChildren(firstOption, ...options);
    if (state.products.some((product) => String(product.id) === selected)) select.value = selected;
  }
}

function renderAlerts() {
  const list = document.querySelector("#alerts-list");
  document.querySelector("#alert-count").textContent =
    `${state.alerts.length} alert${state.alerts.length === 1 ? "" : "s"}`;
  if (!state.alerts.length) {
    const message = document.createElement("p");
    message.className = "empty-state";
    message.textContent = "All products are at or above their reorder level.";
    list.replaceChildren(message);
    return;
  }
  const items = state.alerts.map((alert) => {
    const item = document.createElement("article");
    item.className = "alert-item";
    const name = document.createElement("strong");
    name.textContent = alert.productName;
    const detail = document.createElement("span");
    detail.textContent = `${alert.quantity} in stock · reorder level ${alert.reorderLevel}`;
    item.append(name, detail);
    return item;
  });
  list.replaceChildren(...items);
}

async function renderMovements() {
  const selectedProductId = document.querySelector("#movement-filter").value;
  const movements = selectedProductId
    ? await request(`${api.movements}/product/${selectedProductId}`)
    : state.movements;
  const body = document.querySelector("#movements-table");
  document.querySelector("#movement-count").textContent = state.movements.length.toLocaleString();
  if (!movements.length) {
    emptyRow(body, 5, "No stock movements to display.");
    return;
  }
  const rows = movements.map((movement) => {
    const row = document.createElement("tr");
    row.append(cell(`#${movement.id}`));
    const type = document.createElement("td");
    type.className = `movement-type ${movement.type === "INWARD" ? "movement-in" : "movement-out"}`;
    type.textContent = `${movement.type} · #${movement.productId}`;
    row.append(type, cell(movement.quantity));
    const change = document.createElement("td");
    change.className = "stock-change";
    const before = document.createElement("strong");
    before.textContent = movement.quantityBefore;
    change.append(before, document.createTextNode(" → "));
    const after = document.createElement("strong");
    after.textContent = movement.quantityAfter;
    change.append(after);
    row.append(change);
    const date = new Date(movement.timestamp);
    row.append(cell(Number.isNaN(date.getTime()) ? movement.timestamp : date.toLocaleString()));
    return row;
  });
  body.replaceChildren(...rows);
}

async function loadDashboard({ successMessage } = {}) {
  setLoading(true);
  try {
    const [products, alerts, movements] = await Promise.all([
      request(api.products), request(api.alerts), request(api.movements)
    ]);
    state.products = products;
    state.alerts = alerts;
    state.movements = movements;
    renderProducts();
    renderAlerts();
    await renderMovements();
    if (successMessage) showFeedback(successMessage);
    else hideFeedback();
    return true;
  } catch (error) {
    showFeedback(error.message, "error");
    return false;
  } finally {
    setLoading(false);
  }
}

async function showProductDetails(productId) {
  try {
    const product = await request(`${api.products}/${productId}`);
    const dialog = document.querySelector("#product-dialog");
    document.querySelector("#dialog-product-name").textContent = product.name;
    const fields = [
      ["Product ID", product.id], ["Description", product.description],
      ["Current quantity", product.quantity], ["Reorder level", product.reorderLevel]
    ];
    const details = document.querySelector("#product-details");
    details.replaceChildren(...fields.map(([label, value]) => {
      const wrapper = document.createElement("div");
      const term = document.createElement("dt");
      term.textContent = label;
      const description = document.createElement("dd");
      description.textContent = value;
      wrapper.append(term, description);
      return wrapper;
    }));
    dialog.showModal();
  } catch (error) {
    showFeedback(error.message, "error");
  }
}

document.querySelector("#product-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const submit = form.querySelector("button[type=submit]");
  const values = Object.fromEntries(new FormData(form));
  const product = {
    id: Number(values.id), name: values.name.trim(), description: values.description.trim(),
    quantity: Number(values.quantity), reorderLevel: Number(values.reorderLevel)
  };
  submit.disabled = true;
  try {
    await request(api.products, { method: "POST", body: JSON.stringify(product) });
    form.reset();
    form.elements.quantity.value = 0;
    form.elements.reorderLevel.value = 0;
    await loadDashboard({ successMessage: `${product.name} was added successfully.` });
  } catch (error) {
    showFeedback(error.message, "error");
  } finally {
    submit.disabled = false;
  }
});

document.querySelector("#stock-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const submit = form.querySelector("button[type=submit]");
  const productId = form.elements.productId.value;
  const operation = form.elements.operation.value;
  const quantity = Number(form.elements.quantity.value);
  submit.disabled = true;
  try {
    const updated = await request(`${api.products}/${productId}/stock/${operation}`, {
      method: "PUT", body: JSON.stringify({ quantity })
    });
    form.elements.quantity.value = "";
    await loadDashboard({ successMessage: `Stock updated. ${updated.name} now has ${updated.quantity} units.` });
  } catch (error) {
    showFeedback(error.message, "error");
  } finally {
    submit.disabled = false;
  }
});

document.querySelector("#movement-filter").addEventListener("change", () => {
  renderMovements().catch((error) => showFeedback(error.message, "error"));
});
document.querySelector("#refresh-button").addEventListener("click", () => loadDashboard());
document.querySelector("#close-dialog").addEventListener("click", () => {
  document.querySelector("#product-dialog").close();
});

loadDashboard();
