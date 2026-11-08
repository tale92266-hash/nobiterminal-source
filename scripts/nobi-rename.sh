#!/bin/bash
# NobiTerminal direct-source build: rename com.termux -> com.nobiterminal.
#
# Vendored from the generator (tale92266-hash/nobiterminal, branch
# frozen-v19-base, scripts/termux_generator_utils.sh:
# replace_termux_name + migrate_termux_folder + migrate_termux_folder_tree),
# logic unchanged. The snapshot tree in nobiterminal-source stays com.termux;
# the rename happens HERE, at build time, exactly as the generator did it
# for v4..v24. Two deliberate differences from the vendored original:
#  - find excludes ./.git (the Actions checkout has .git metadata; the
#    generator's clones were processed the same way only by luck of pattern
#    mismatch — excluding it is strictly safer).
#  - target dir is the repo root (the termux-app tree itself), not a
#    termux-apps-main parent dir.
#
# Usage (from the repo root): bash scripts/nobi-rename.sh
set -e -u -o pipefail

PACKAGE_NAME="com.nobiterminal"

portable_sed_i() {
    if sed v </dev/null 2> /dev/null; then
        sed -i "$@"
    else
        sed -i '' "$@"
    fi
}

replace_termux_name() {
    if [[ "$PACKAGE_NAME" == "com.termux" ]]; then
        return
    fi
    local targetdir="$1"
    local replacement_name="$2"
    local replacement_name_underscore="$(echo "$replacement_name" | tr . _)"
    local replacement_name_slash="$(echo "$replacement_name" | tr . /)"

    pushd "$targetdir" >/dev/null

    # Only text files, to avoid errors on binaries.
    local file
    find . -type f -not -path './.git/*' -exec file {} + | grep "text" | cut -d: -f1 | while read -r file; do
        portable_sed_i -e "s|>Termux<|>$replacement_name<|g" \
                       -e "s|\"Termux\"|\"$replacement_name\"|g" \
                       -e "s|Termux:|$replacement_name:|g" \
                       -e "s|com\.termux|$replacement_name|g" \
                       -e "s|com_termux|$replacement_name_underscore|g" \
                       -e '/http/!s|com/termux|'$replacement_name_slash'|g' "$file"
    done

    popd >/dev/null
}

# Function to migrate a folder (com/termux -> com/nobiterminal)
migrate_termux_folder() {
    if [[ "$PACKAGE_NAME" == "com.termux" ]]; then
        return
    fi
    local parentdir="$(dirname "$(dirname "$1")")"
    local replacement_name="$2"
    local destination="${parentdir}/$(echo "$replacement_name" | tr . /)/"

    echo "Migrating folder:"
    echo "- ${parentdir}/com/termux/"
    echo "to"
    echo "+ ${destination}"
    mkdir -p "${destination}"
    mv "${parentdir}/com/termux/"* "${destination}"
    rm -r "${parentdir}/com/termux/"
}

migrate_termux_folder_tree() {
    if [[ "$PACKAGE_NAME" == "com.termux" ]]; then
        return
    fi
    local targetdir="$1"
    local replacement_name="$2"

    pushd "$targetdir" >/dev/null

    local dir
    find "$(pwd)" -type d -name termux | grep -v -e 'shared/termux' -e 'settings/termux' | while read -r dir; do
        migrate_termux_folder "$dir" "$replacement_name"
    done

    popd >/dev/null
}

replace_termux_name "." "$PACKAGE_NAME"
migrate_termux_folder_tree "." "$PACKAGE_NAME"
echo "Rename done: com.termux -> $PACKAGE_NAME"
