/**
 * WelcomeController: radio welcome controller.
 * Copyright (c) 2018 Sascha Baumeister
 */
"use strict";

(function () {
	// imports
	const Controller = de_sb_radio.Controller;


	/**
	 * Creates a new welcome controller that is derived from an abstract controller.
	 */
	const WelcomeController = de_sb_radio.WelcomeController = function () {
		Controller.call(this);
	}
	WelcomeController.prototype = Object.create(Controller.prototype);
	WelcomeController.prototype.constructor = WelcomeController;


	/**
	 * Displays the associated view.
	 */
	Object.defineProperty(WelcomeController.prototype, "display", {
		enumerable: false,
		configurable: false,
		writable: true,
		value: function () {
			Controller.sessionOwner = null;

			const mainElement = document.querySelector("main");
			mainElement.appendChild(document.querySelector("#login-template").content.cloneNode(true).firstElementChild);
			mainElement.querySelector("button").addEventListener("click", event => this.login());
			this.login();
		}
	});


	/**
	 * Performs a login check on the given user data, assigns the controller's
	 * user object if the login was successful, and initiates rendering of the
	 * message view.
	 */
	Object.defineProperty(WelcomeController.prototype, "login", {
		enumerable: false,
		configurable: false,
		value: async function () {
			this.displayError();

			try {
				const inputElements = document.querySelectorAll("section.login input");
				const email = inputElements[0].value.trim();
				const password = inputElements[1].value.trim();
				
				
				// saved and then remove. 
				 inputElements[0].value = "ines.bergmann@web.de";
				 inputElements[1].value = "ines";

				// Although fetch() supports sending credentials from a browser's hidden Basic-Auth credentials store, it lacks
				// support for storing them securely. This workaround uses a classic XMLHttpRequest invocation as a workaround.
				Controller.sessionOwner = JSON.parse(await this.xhr("/services/people/0", "GET", {"Accept": "application/json"}, "", "text", email, password));

				let anchor = document.querySelector("header li:nth-of-type(2) > a");
				anchor.dispatchEvent(new MouseEvent("click")); 
			} catch (error) {
				this.displayError(error);
			}
		}
	});


	/**
	 * Perform controller callback registration during DOM load event handling.
	 */
	window.addEventListener("load", event => {
		const anchor = document.querySelector("header li:nth-of-type(1) > a");
		const controller = new WelcomeController();
		anchor.addEventListener("click", event => controller.display());

		anchor.dispatchEvent(new MouseEvent("click"));
	});
} ());