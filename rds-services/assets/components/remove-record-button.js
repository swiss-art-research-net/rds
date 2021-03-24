class RemoveRecordButton extends HTMLElement {
    constructor() {
      super();
    }
    static get observedAttributes(){
      return ['storage', 'repository', 'resourceIri', 'fileName'];
    }
    attributeChangedCallback(name, oldValue, newValue) {
      this.render();
    }
    connectedCallback(){
      this.render();
    }

    render() {
      this.innerHTML = `<div>
        <button
            class='btn btn-sm btn-danger'
            style='margin-right: 8px;'>
            <i class='fa fa-trash' aria-hidden=true></i> Delete
        </button>
      </div>`;
      const button = this.querySelector('.btn.btn-sm.btn-danger');
      button.addEventListener('click', () => this.removeElement())
    }

    removeElement() {
      const formData = new FormData();
      for(const attrName of RemoveRecordButton.observedAttributes) {
        formData.append(attrName, this.getAttribute(attrName))
      }
      fetch('/file', {method: 'DELETE', body: formData}).then(response => {
        console.log(`File "${this.getAttribute('fileName')}" have been deleted.`);
        window.location.reload();
      });
    }
  }

customElements.define('remove-record-button', RemoveRecordButton);