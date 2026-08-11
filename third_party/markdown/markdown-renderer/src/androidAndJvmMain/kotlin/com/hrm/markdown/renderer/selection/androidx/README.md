# Markdown selection engine fork

This directory is based on Compose Foundation 1.11.1's `SelectionContainer`,
`SelectionManager`, and `SelectionRegistrarImpl` under the Apache 2.0 license retained in each
source file.

Compose binds `SelectionManager` to the concrete registrar and documents selection over lazy,
uncomposed content as undefined. Markdown therefore cannot replace only the registrar through a
public API. The fork keeps the upstream gesture, highlight, handle, toolbar, and clipboard state
machine, while its registrar sorts persistent selectable proxies by the renderer's explicit
document order instead of transient layout coordinates.

Do not replace this with invisible duplicate text, placeholder coordinates, or registration-order
sorting. When upgrading Compose, diff all forked files against the matching upstream version and
reapply only the document-order registrar boundary. Android and Compose JVM use this fork; iOS keeps
the platform SelectionContainer until the Native compiler can safely consume the same internal
selection boundary.
