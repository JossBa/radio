/**
 * ServerRadioController: server radio controller.
 */
"use strict";

(function () {
	// imports
	const Controller = de_sb_radio.Controller;
	let AUDIO_CONTEXT_CONSTRUCTOR = window.AudioContext || window.webkitAudioContext;



	/**
	 * Creates a new server radio controller that is derived from an abstract controller.
	 */
	const ServerRadioController = function () {
		Controller.call(this);

		let localTracks = [];
		Object.defineProperty(this, "tracks", {
			enumerable: true,
			configurable: false,
			get: function () {
				return localTracks;
			}
		});

		Object.defineProperty(this, "trackPosition", {
			enumerable: true,
			configurable: false,
			writable: true,
			value: -1
		});
	}
	ServerRadioController.prototype = Object.create(Controller.prototype);
	ServerRadioController.prototype.constructor = ServerRadioController;


	/**
	 * Displays the associated view.
	 */
	Object.defineProperty(ServerRadioController.prototype, "display", {
		enumerable: false,
		configurable: false,
		writable: true,
		value: async function () {
			if (!Controller.sessionOwner) {
				const anchor = document.querySelector("header li:first-of-type > a");
				anchor.dispatchEvent(new MouseEvent("click"));
				return;
			}

			try {
				let mainElement = document.querySelector("main");
				let sectionElement = document.querySelector("#server-radio-template").content.cloneNode(true).firstElementChild;
				let response, selectElement;

				response = await fetch("/services/tracks/genres", { method: "GET", credentials: "include", headers: { Accept: "application/json"}});
				if (!response.ok) throw new Error("HTTP " + response.status + " " + response.statusText);
				const genres = await response.json();

				selectElement = sectionElement.querySelector('select[name="genres-select"]');
				for (let genre of genres) {
					let optionElement = document.createElement("option");
					optionElement.appendChild(document.createTextNode(genre));
					optionElement.value = genre;
					selectElement.appendChild(optionElement);
				}

				response = await fetch("/services/tracks/artists?resultOffset=0&resultLimit=100", {
					method: "GET",
					credentials: "include",
					headers: {
						accept: "application/json"
					}
				});
				if (!response.ok) throw new Error("HTTP " + response.status + " " + response.statusText);
				const artists = await response.json();

				selectElement = sectionElement.querySelector('select[name="artists-select"]');
				for (let artist of artists) {
					let optionElement = document.createElement("option");
					optionElement.appendChild(document.createTextNode(artist));
					optionElement.value = artist;
					selectElement.appendChild(optionElement);
				}

				mainElement.appendChild(sectionElement);
				mainElement.querySelector("button").addEventListener("click", event => this.startRadio());
			} catch (error) {
				this.displayError(error);
			}
		}
	});


	/**
	 * Performs a login check on the given user data, assigns the controller's
	 * user object if the login was successful, and initiates rendering of the
	 * message view.
	 */
	Object.defineProperty(ServerRadioController.prototype, "startRadio", {
		enumerable: false,
		configurable: false,
		value: async function () {
			this.displayError();

			try {
				let uri = "/services/tracks";
				let sectionElement = document.querySelector("main > section");
				let genreSelectElement = sectionElement.querySelector('select[name="genres-select"]');
				let artistSelectElement = sectionElement.querySelector('select[name="artists-select"]');
				let genreElements = genreSelectElement.querySelectorAll("option");
				let artistElements = artistSelectElement.querySelectorAll("option");
				let playlistTable = sectionElement.querySelectorAll("table");

				if (genreElements.length > 0 | artistElements.length > 0) uri += "?";

				for (let genreElement of genreElements) {
					if (genreElement.selected) {
						uri += "genre=" + genreElement.text + "&";
					}
				}

				for (let artistElement of artistElements) {
					if (artistElement.selected) {
						uri += "artist=" + artistElement.text + "&";
					}
				}

				if (uri.endsWith("&")) uri = uri.substring(0, uri.length - 1);
				let response = await fetch(uri, {
					method: "GET",
					credentials: "include",
					headers: {
						accept: "application/json"
					}
				});
				if (!response.ok) throw new Error("HTTP " + response.status + " " + response.statusText);
				const tracks = await response.json();
				Array.prototype.push.apply(this.tracks, tracks);

				let tableElement = document.createElement("table");
				sectionElement.appendChild(tableElement);

				for (let track of this.tracks) {
					let rowElement = document.createElement("tr");
					let cellElement;

					let coverElement = document.createElement("img");				
				    const uriAlbum = "/services/albums/" + track.albumReference;
					let response = await fetch(uriAlbum, { method: "GET", credentials: "include", headers: {Accept: "application/json"}});
					if (!response.ok) throw new Error("HTTP " + response.status + " " + response.statusText);
					const album = await response.json();
					
					coverElement.src =  "/services/documents/" + album.coverReference + "?width=80&height=80";
					cellElement = document.createElement("td");
					cellElement.appendChild(coverElement);
					rowElement.appendChild(cellElement);

					cellElement = document.createElement("td");
					cellElement.appendChild(document.createTextNode(track.artist));
					rowElement.appendChild(cellElement);
					
					cellElement = document.createElement("td");
					cellElement.appendChild(document.createTextNode(track.name));
					rowElement.appendChild(cellElement);

					tableElement.appendChild(rowElement);
				}

				this.playNextTrack();

			} catch (error) {
				this.displayError(error);
			}
		}
	});


	Object.defineProperty(ServerRadioController.prototype, "playNextTrack", {
		enumerable: false,
		configurable: false,
		value: async function () {
			if (this.tracks.length - 1 == this.trackPosition) return;
			const track = this.tracks[++this.trackPosition];

			const uri = "/services/documents/" + track.recordingReference;
			let response = await fetch(uri, { method: "GET", credentials: "include", headers: { Accept: "audio/*"}});
			if (!response.ok) throw new Error("HTTP " + response.status + " " + response.statusText);
			
			const audioBuffer = await response.arrayBuffer();
			const decodedBuffer = await Controller.audioContext.decodeAudioData(audioBuffer);
			let audioSource = Controller.audioContext.createBufferSource();
			audioSource.loop = false;
			audioSource.buffer = decodedBuffer;
			audioSource.connect(Controller.audioContext.destination);
			audioSource.onended = () => this.playNextTrack();
			audioSource.start();

			// audioSource.onended = this.playNextTrack.bind(this); alternative zur Lambda Expression			
		}
	});


	/**
	 * Perform controller callback registration during DOM load event handling.
	 */
	window.addEventListener("load", event => {
		const anchor = document.querySelector("header li:nth-of-type(2) > a");
		const controller = new ServerRadioController();
		anchor.addEventListener("click", event => controller.display());
	});
}());