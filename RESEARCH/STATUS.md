# Status

## v1.9.0 → v1.10.0

### Completed
- Canvas background: removed layered shadows, edge stroke, and top sheen from PageBackgroundRenderer — paper fills the viewport cleanly
- Pen color picker: moved PenPickersPanel from bottom of canvas to below the toolbar (above canvas)
- Palm rejection cold-start: relaxed distance-based CANDIDATE promotion guard from 1.1x to 1.2x of configured minimum velocity
- Auto-hide scroll bar while writing (scrollBarVisible = !inkActive)
- Cleaned up dead code: removed DummyContextPanelContent and unused ContextPanelRow placeholder

### All 443 unit tests passing

### Done
- v1.9.0 released (commit c9eeb2a)
