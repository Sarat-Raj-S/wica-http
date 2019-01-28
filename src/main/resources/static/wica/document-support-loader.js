/**
 * Loads the services that are required to provide Wica support for the current HTML document.
 * @module
 */
console.debug( "Executing script in document-support-loader.js module...");

import {WicaElementConnectionAttributes,
        WicaElementRenderingAttributes,
        WicaElementEventManagerAttributes,
        WicaStreamProperties} from "./shared-definitions.js";

import {DocumentStreamConnector} from "./document-stream-connector.js";
import {DocumentTextRenderer} from "./document-text-renderer.js";
import {DocumentEventManager} from "./document-event-manager.js";

const WICA_HOST="https://gfa-wica-dev.psi.ch";

const documentStreamConnector = new DocumentStreamConnector( WICA_HOST, WicaStreamProperties, WicaElementConnectionAttributes );
const documentTextRenderer = new DocumentTextRenderer( WicaElementConnectionAttributes, WicaElementRenderingAttributes );
const documentEventManager = new DocumentEventManager( WicaElementEventManagerAttributes, WicaWicaElementConnectionAttributes );

/**
 * Loads support for the current document.
 *
 * @param {number} [textRendererRefreshRate=100] - the rate at which the document's text renderer should run to update the
 *     visual state of the document's wica-aware elements.
 *
 * @param {number} [eventManagerRefreshRate=100] - the rate at which the document's event manager should run to fire
 *    notification events on the state of the document's wica-aware elements.
 */
export function load( textRendererRefreshRate = 100, eventManagerRefreshRate = 100 )
{
    loadWicaCSS_();
    documentStreamConnector.activate();
    documentTextRenderer.activate( textRendererRefreshRate );
    documentEventManager.activate( eventManagerRefreshRate);
}

/**
 * Unloads support for the current document.
 */
export function unload()
{
    documentStreamConnector.shutdown();
    documentTextRenderer.shutdown();
    documentEventManager.shutdown();
}


/**
 * @private
 */
function loadWicaCSS_()
{
    if ( !document.getElementById('wica-css-id' ) )
    {
        const head = document.getElementsByTagName('head')[0];
        const link = document.createElement('link');
        link.id = 'wica-css-id';
        link.rel = 'stylesheet';
        link.type = 'text/css';
        link.href = WICA_HOST + '/wica/wica.css';
        link.media = 'all';
        head.appendChild(link);
    }
}
