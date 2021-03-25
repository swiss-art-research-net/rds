class ExportRecordsButton extends HTMLElement {
  constructor() {
    super();
  }
  static get observedAttributes(){
    return ["comment"];
  }
  attributeChangedCallback(name, oldValue, newValue) {
    this.render();
  }
  connectedCallback(){
    this.render();
  }

  render() {
    this.innerHTML = `<div style="margin-left: 10px">
      <button name="submit" class="btn btn-primary">Export records</button>
      <select id="mediaType">
        <option value="turtle">Turtle</option>
        <option value="rdf/xml">RDF/XML</option>
        <option value="json-ld">JSON-LD</option>
      </select>
    </div>`;
    const button = this.querySelector('.btn.btn-primary');
    const select = this.querySelector('#mediaType');
    button.addEventListener('click', () => this.pushRecords(select.value))
  }

  pushRecords(mediaType) {
    const urlEncodedBody = new URLSearchParams();
    for(const attrName of ExportRecordsButton.observedAttributes) {
      urlEncodedBody.append(attrName, this.getAttribute(attrName))
    }
    if (mediaType) {
      urlEncodedBody.append('mediaType', mediaType);
    }
    fetch('/rest/extension/records/export', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
      },
      body: urlEncodedBody,
    }).then(response => {
      console.log(`${response.status}:${response.text}`);
      if (response.status === 200) {
        console.log(`Records processing started.`);
      }
      window.location.reload();
    });
  }
}

customElements.define('export-records-button', ExportRecordsButton);