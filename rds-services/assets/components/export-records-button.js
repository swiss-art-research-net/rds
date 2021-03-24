class ExportRecordsButton extends HTMLElement {
    constructor() {
      super();
    }
    static get observedAttributes(){
      return [];
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
      </div>`;
      const button = this.querySelector('.btn.btn-primary');
      button.addEventListener('click', () => this.pushRecords())
    }

    pushRecords() {
      fetch('/rest/extension/records/export', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
        }}
      ).then(response => {
        console.log(`Records processing started.`);
        window.location.reload();
      });
    }
  }

customElements.define('export-records-button', ExportRecordsButton);