package com.webscorer.service;

/**
 * Result of a successful, safety-checked HTML download.
 *
 * @param doc        parsed document
 * @param status     final HTTP status code
 * @param finalUrl   URL after following redirects
 * @param durationMs wall-clock duration of the whole fetch
 * @param bytes      number of body bytes actually read (capped by maxBytes)
 */
public record FetchedPage(org.jsoup.nodes.Document doc, int status, String finalUrl, long durationMs, int bytes) {
}
