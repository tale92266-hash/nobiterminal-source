/**
 * Available app icons that can be selected from the settings.
 * The `id` matches the icon names accepted by `system.setAppIcon`.
 * The `image` is a relative path (relative to the www root) to an SVG
 * preview that is rendered inside the UI.
 */

// Update system.java map too if this is updated
export const APP_ICONS = [
	{ id: "default", label: "Default", image: "icons/ic_nobicode_default.svg" },
];

export const APP_ICON_IDS = APP_ICONS.map((icon) => icon.id);
