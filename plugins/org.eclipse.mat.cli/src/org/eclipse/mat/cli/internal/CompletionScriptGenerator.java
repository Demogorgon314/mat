/*******************************************************************************
 * Copyright (c) 2026 Eclipse Memory Analyzer Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.mat.cli.internal;

import java.util.ArrayList;
import java.util.List;

public final class CompletionScriptGenerator
{
    public String generate(String shell) throws CliException
    {
        if ("bash".equals(shell)) //$NON-NLS-1$
            return generateBash();
        if ("zsh".equals(shell)) //$NON-NLS-1$
            return generateZsh();
        throw CliException.usage("Unsupported completion shell: " + shell + " (expected bash or zsh)"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String generateBash()
    {
        StringBuilder builder = new StringBuilder(16384);
        builder.append("# bash completion for mat-cli\n"); //$NON-NLS-1$
        builder.append("_mat_cli_is_command() {\n"); //$NON-NLS-1$
        builder.append("    case \"$1\" in\n"); //$NON-NLS-1$
        builder.append("        ").append(joinWithPipe(CliCommandCatalog.commandTokens())).append(")\n"); //$NON-NLS-1$
        builder.append("            return 0\n"); //$NON-NLS-1$
        builder.append("            ;;\n"); //$NON-NLS-1$
        builder.append("    esac\n"); //$NON-NLS-1$
        builder.append("    return 1\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$

        appendKindHelpers(builder, false);
        appendOptionKindFunctions(builder, false);
        appendOptionsForCommandFunction(builder, false);
        appendPositionalKindFunction(builder, false);

        builder.append("_mat_cli_set_word_replies() {\n"); //$NON-NLS-1$
        builder.append("    local prefix=\"$1\"\n"); //$NON-NLS-1$
        builder.append("    local current=\"$2\"\n"); //$NON-NLS-1$
        builder.append("    local values=\"$3\"\n"); //$NON-NLS-1$
        builder.append("    local i\n"); //$NON-NLS-1$
        builder.append("    COMPREPLY=( $(compgen -W \"$values\" -- \"$current\") )\n"); //$NON-NLS-1$
        builder.append("    if [ -n \"$prefix\" ]; then\n"); //$NON-NLS-1$
        builder.append("        for ((i=0; i<${#COMPREPLY[@]}; i++)); do\n"); //$NON-NLS-1$
        builder.append("            COMPREPLY[i]=\"$prefix${COMPREPLY[i]}\"\n"); //$NON-NLS-1$
        builder.append("        done\n"); //$NON-NLS-1$
        builder.append("    fi\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$

        builder.append("_mat_cli_set_file_replies() {\n"); //$NON-NLS-1$
        builder.append("    local prefix=\"$1\"\n"); //$NON-NLS-1$
        builder.append("    local current=\"$2\"\n"); //$NON-NLS-1$
        builder.append("    local line\n"); //$NON-NLS-1$
        builder.append("    COMPREPLY=()\n"); //$NON-NLS-1$
        builder.append("    while IFS= read -r line; do\n"); //$NON-NLS-1$
        builder.append("        if [ -n \"$prefix\" ]; then\n"); //$NON-NLS-1$
        builder.append("            COMPREPLY+=(\"$prefix$line\")\n"); //$NON-NLS-1$
        builder.append("        else\n"); //$NON-NLS-1$
        builder.append("            COMPREPLY+=(\"$line\")\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("    done < <(compgen -f -- \"$current\")\n"); //$NON-NLS-1$
        builder.append("    if type compopt >/dev/null 2>&1; then\n"); //$NON-NLS-1$
        builder.append("        compopt -o filenames 2>/dev/null\n"); //$NON-NLS-1$
        builder.append("    fi\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$

        builder.append("_mat_cli_complete_kind() {\n"); //$NON-NLS-1$
        builder.append("    local kind=\"$1\"\n"); //$NON-NLS-1$
        builder.append("    local current=\"$2\"\n"); //$NON-NLS-1$
        builder.append("    local prefix=\"$3\"\n"); //$NON-NLS-1$
        builder.append("    case \"$kind\" in\n"); //$NON-NLS-1$
        builder.append("        enum:*)\n"); //$NON-NLS-1$
        builder.append("            _mat_cli_set_word_replies \"$prefix\" \"$current\" \"${kind#enum:}\"\n"); //$NON-NLS-1$
        builder.append("            ;;\n"); //$NON-NLS-1$
        builder.append("        file)\n"); //$NON-NLS-1$
        builder.append("            _mat_cli_set_file_replies \"$prefix\" \"$current\"\n"); //$NON-NLS-1$
        builder.append("            ;;\n"); //$NON-NLS-1$
        builder.append("        *)\n"); //$NON-NLS-1$
        builder.append("            COMPREPLY=()\n"); //$NON-NLS-1$
        builder.append("            ;;\n"); //$NON-NLS-1$
        builder.append("    esac\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$

        builder.append("_mat_cli_find_command_index() {\n"); //$NON-NLS-1$
        builder.append("    local i word pending_kind=\"\"\n"); //$NON-NLS-1$
        builder.append("    for ((i=1; i<COMP_CWORD; i++)); do\n"); //$NON-NLS-1$
        builder.append("        word=\"${COMP_WORDS[i]}\"\n"); //$NON-NLS-1$
        builder.append("        if [ -n \"$pending_kind\" ]; then\n"); //$NON-NLS-1$
        builder.append("            pending_kind=\"\"\n"); //$NON-NLS-1$
        builder.append("            continue\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        if _mat_cli_is_command \"$word\"; then\n"); //$NON-NLS-1$
        builder.append("            printf '%s\\n' \"$i\"\n"); //$NON-NLS-1$
        builder.append("            return 0\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        if [[ \"$word\" == --*=* ]]; then\n"); //$NON-NLS-1$
        builder.append("            continue\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        pending_kind=\"$(_mat_cli_option_kind_global \"$word\")\"\n"); //$NON-NLS-1$
        builder.append("        if ! _mat_cli_kind_expects_value \"$pending_kind\"; then\n"); //$NON-NLS-1$
        builder.append("            pending_kind=\"\"\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("    done\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$

        builder.append("_mat_cli_completion() {\n"); //$NON-NLS-1$
        builder.append("    local cur prev cmd_index cmd position_index pending_kind inline_option inline_prefix kind options\n"); //$NON-NLS-1$
        builder.append("    local i word positional_kind\n"); //$NON-NLS-1$
        builder.append("    COMPREPLY=()\n"); //$NON-NLS-1$
        builder.append("    cur=\"${COMP_WORDS[COMP_CWORD]}\"\n"); //$NON-NLS-1$
        builder.append("    prev=\"${COMP_WORDS[COMP_CWORD-1]}\"\n"); //$NON-NLS-1$
        builder.append("    cmd_index=\"$(_mat_cli_find_command_index)\"\n"); //$NON-NLS-1$
        builder.append("\n"); //$NON-NLS-1$
        builder.append("    if [ -z \"$cmd_index\" ]; then\n"); //$NON-NLS-1$
        builder.append("        if [[ \"$cur\" == --*=* ]]; then\n"); //$NON-NLS-1$
        builder.append("            inline_option=\"${cur%%=*}\"\n"); //$NON-NLS-1$
        builder.append("            inline_prefix=\"$inline_option=\"\n"); //$NON-NLS-1$
        builder.append("            kind=\"$(_mat_cli_option_kind_global \"$inline_option\")\"\n"); //$NON-NLS-1$
        builder.append("            if _mat_cli_kind_expects_value \"$kind\"; then\n"); //$NON-NLS-1$
        builder.append("                _mat_cli_complete_kind \"$kind\" \"${cur#*=}\" \"$inline_prefix\"\n"); //$NON-NLS-1$
        builder.append("                return 0\n"); //$NON-NLS-1$
        builder.append("            fi\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        kind=\"$(_mat_cli_option_kind_global \"$prev\")\"\n"); //$NON-NLS-1$
        builder.append("        if _mat_cli_kind_expects_value \"$kind\"; then\n"); //$NON-NLS-1$
        builder.append("            _mat_cli_complete_kind \"$kind\" \"$cur\" \"\"\n"); //$NON-NLS-1$
        builder.append("            return 0\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        _mat_cli_set_word_replies \"\" \"$cur\" \"") //$NON-NLS-1$
                        .append(joinWords(topLevelWords())).append("\"\n"); //$NON-NLS-1$
        builder.append("        return 0\n"); //$NON-NLS-1$
        builder.append("    fi\n\n"); //$NON-NLS-1$

        builder.append("    cmd=\"${COMP_WORDS[cmd_index]}\"\n"); //$NON-NLS-1$
        builder.append("    if [[ \"$cur\" == --*=* ]]; then\n"); //$NON-NLS-1$
        builder.append("        inline_option=\"${cur%%=*}\"\n"); //$NON-NLS-1$
        builder.append("        inline_prefix=\"$inline_option=\"\n"); //$NON-NLS-1$
        builder.append("        kind=\"$(_mat_cli_option_kind \"$cmd\" \"$inline_option\")\"\n"); //$NON-NLS-1$
        builder.append("        if _mat_cli_kind_expects_value \"$kind\"; then\n"); //$NON-NLS-1$
        builder.append("            _mat_cli_complete_kind \"$kind\" \"${cur#*=}\" \"$inline_prefix\"\n"); //$NON-NLS-1$
        builder.append("            return 0\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("    fi\n"); //$NON-NLS-1$
        builder.append("    kind=\"$(_mat_cli_option_kind \"$cmd\" \"$prev\")\"\n"); //$NON-NLS-1$
        builder.append("    if _mat_cli_kind_expects_value \"$kind\"; then\n"); //$NON-NLS-1$
        builder.append("        _mat_cli_complete_kind \"$kind\" \"$cur\" \"\"\n"); //$NON-NLS-1$
        builder.append("        return 0\n"); //$NON-NLS-1$
        builder.append("    fi\n\n"); //$NON-NLS-1$

        builder.append("    position_index=0\n"); //$NON-NLS-1$
        builder.append("    pending_kind=\"\"\n"); //$NON-NLS-1$
        builder.append("    for ((i=cmd_index+1; i<COMP_CWORD; i++)); do\n"); //$NON-NLS-1$
        builder.append("        word=\"${COMP_WORDS[i]}\"\n"); //$NON-NLS-1$
        builder.append("        if [ -n \"$pending_kind\" ]; then\n"); //$NON-NLS-1$
        builder.append("            pending_kind=\"\"\n"); //$NON-NLS-1$
        builder.append("            continue\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        if [[ \"$word\" == --*=* ]]; then\n"); //$NON-NLS-1$
        builder.append("            continue\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        kind=\"$(_mat_cli_option_kind \"$cmd\" \"$word\")\"\n"); //$NON-NLS-1$
        builder.append("        if [ -n \"$kind\" ]; then\n"); //$NON-NLS-1$
        builder.append("            if _mat_cli_kind_expects_value \"$kind\"; then\n"); //$NON-NLS-1$
        builder.append("                pending_kind=\"$kind\"\n"); //$NON-NLS-1$
        builder.append("            fi\n"); //$NON-NLS-1$
        builder.append("            continue\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        position_index=$((position_index + 1))\n"); //$NON-NLS-1$
        builder.append("    done\n\n"); //$NON-NLS-1$

        builder.append("    if [[ \"$cur\" == -* ]]; then\n"); //$NON-NLS-1$
        builder.append("        options=\"$(_mat_cli_options_for_command \"$cmd\")\"\n"); //$NON-NLS-1$
        builder.append("        _mat_cli_set_word_replies \"\" \"$cur\" \"$options\"\n"); //$NON-NLS-1$
        builder.append("        return 0\n"); //$NON-NLS-1$
        builder.append("    fi\n\n"); //$NON-NLS-1$

        builder.append("    positional_kind=\"$(_mat_cli_positional_kind \"$cmd\" \"$position_index\")\"\n"); //$NON-NLS-1$
        builder.append("    case \"$positional_kind\" in\n"); //$NON-NLS-1$
        builder.append("        enum:*|file)\n"); //$NON-NLS-1$
        builder.append("            _mat_cli_complete_kind \"$positional_kind\" \"$cur\" \"\"\n"); //$NON-NLS-1$
        builder.append("            ;;\n"); //$NON-NLS-1$
        builder.append("        *)\n"); //$NON-NLS-1$
        builder.append("            options=\"$(_mat_cli_options_for_command \"$cmd\")\"\n"); //$NON-NLS-1$
        builder.append("            _mat_cli_set_word_replies \"\" \"$cur\" \"$options\"\n"); //$NON-NLS-1$
        builder.append("            ;;\n"); //$NON-NLS-1$
        builder.append("    esac\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$
        builder.append("complete -F _mat_cli_completion mat-cli\n"); //$NON-NLS-1$
        return builder.toString();
    }

    private String generateZsh()
    {
        StringBuilder builder = new StringBuilder(16384);
        builder.append("#compdef mat-cli\n"); //$NON-NLS-1$
        builder.append("# zsh completion for mat-cli\n"); //$NON-NLS-1$
        builder.append("_mat_cli_is_command() {\n"); //$NON-NLS-1$
        builder.append("    case \"$1\" in\n"); //$NON-NLS-1$
        builder.append("        ").append(joinWithPipe(CliCommandCatalog.commandTokens())).append(")\n"); //$NON-NLS-1$
        builder.append("            return 0\n"); //$NON-NLS-1$
        builder.append("            ;;\n"); //$NON-NLS-1$
        builder.append("    esac\n"); //$NON-NLS-1$
        builder.append("    return 1\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$

        appendKindHelpers(builder, true);
        appendOptionKindFunctions(builder, true);
        appendOptionsForCommandFunction(builder, true);
        appendPositionalKindFunction(builder, true);

        builder.append("_mat_cli_complete_words() {\n"); //$NON-NLS-1$
        builder.append("    local prefix=\"$1\"\n"); //$NON-NLS-1$
        builder.append("    shift\n"); //$NON-NLS-1$
        builder.append("    if [[ -n \"$prefix\" ]]; then\n"); //$NON-NLS-1$
        builder.append("        compadd -Q -P \"$prefix\" -- \"$@\"\n"); //$NON-NLS-1$
        builder.append("    else\n"); //$NON-NLS-1$
        builder.append("        compadd -Q -- \"$@\"\n"); //$NON-NLS-1$
        builder.append("    fi\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$

        builder.append("_mat_cli_complete_kind() {\n"); //$NON-NLS-1$
        builder.append("    local kind=\"$1\"\n"); //$NON-NLS-1$
        builder.append("    local prefix=\"$2\"\n"); //$NON-NLS-1$
        builder.append("    local values_string\n"); //$NON-NLS-1$
        builder.append("    local -a values\n"); //$NON-NLS-1$
        builder.append("    case \"$kind\" in\n"); //$NON-NLS-1$
        builder.append("        enum:*)\n"); //$NON-NLS-1$
        builder.append("            values_string=\"${kind#enum:}\"\n"); //$NON-NLS-1$
        builder.append("            values=(${=values_string})\n"); //$NON-NLS-1$
        builder.append("            _mat_cli_complete_words \"$prefix\" \"${values[@]}\"\n"); //$NON-NLS-1$
        builder.append("            ;;\n"); //$NON-NLS-1$
        builder.append("        file)\n"); //$NON-NLS-1$
        builder.append("            if [[ -n \"$prefix\" ]]; then\n"); //$NON-NLS-1$
        builder.append("                compset -P \"$prefix\" >/dev/null 2>&1\n"); //$NON-NLS-1$
        builder.append("                _files -P \"$prefix\"\n"); //$NON-NLS-1$
        builder.append("            else\n"); //$NON-NLS-1$
        builder.append("                _files\n"); //$NON-NLS-1$
        builder.append("            fi\n"); //$NON-NLS-1$
        builder.append("            ;;\n"); //$NON-NLS-1$
        builder.append("        *)\n"); //$NON-NLS-1$
        builder.append("            return 1\n"); //$NON-NLS-1$
        builder.append("            ;;\n"); //$NON-NLS-1$
        builder.append("    esac\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$

        builder.append("_mat_cli_find_command() {\n"); //$NON-NLS-1$
        builder.append("    local i word pending_kind=\"\"\n"); //$NON-NLS-1$
        builder.append("    reply=()\n"); //$NON-NLS-1$
        builder.append("    for ((i=2; i<CURRENT; i++)); do\n"); //$NON-NLS-1$
        builder.append("        word=\"$words[i]\"\n"); //$NON-NLS-1$
        builder.append("        if [[ -n \"$pending_kind\" ]]; then\n"); //$NON-NLS-1$
        builder.append("            pending_kind=\"\"\n"); //$NON-NLS-1$
        builder.append("            continue\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        if _mat_cli_is_command \"$word\"; then\n"); //$NON-NLS-1$
        builder.append("            reply=(\"$word\" \"$i\")\n"); //$NON-NLS-1$
        builder.append("            return 0\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        if [[ \"$word\" == --*=* ]]; then\n"); //$NON-NLS-1$
        builder.append("            continue\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        pending_kind=\"$(_mat_cli_option_kind_global \"$word\")\"\n"); //$NON-NLS-1$
        builder.append("        if ! _mat_cli_kind_expects_value \"$pending_kind\"; then\n"); //$NON-NLS-1$
        builder.append("            pending_kind=\"\"\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("    done\n"); //$NON-NLS-1$
        builder.append("    return 1\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$

        builder.append("_mat-cli() {\n"); //$NON-NLS-1$
        builder.append("    local cur prev cmd cmd_index position_index pending_kind inline_option inline_prefix kind positional_kind options_string\n"); //$NON-NLS-1$
        builder.append("    local i word values_string\n"); //$NON-NLS-1$
        builder.append("    local -a values matches options\n"); //$NON-NLS-1$
        builder.append("    cur=\"$words[CURRENT]\"\n"); //$NON-NLS-1$
        builder.append("    prev=\"$words[CURRENT-1]\"\n"); //$NON-NLS-1$
        builder.append("\n"); //$NON-NLS-1$
        builder.append("    if ! _mat_cli_find_command; then\n"); //$NON-NLS-1$
        builder.append("        if [[ \"$cur\" == --*=* ]]; then\n"); //$NON-NLS-1$
        builder.append("            inline_option=\"${cur%%=*}\"\n"); //$NON-NLS-1$
        builder.append("            inline_prefix=\"$inline_option=\"\n"); //$NON-NLS-1$
        builder.append("            kind=\"$(_mat_cli_option_kind_global \"$inline_option\")\"\n"); //$NON-NLS-1$
        builder.append("            if [[ \"$kind\" == enum:* ]]; then\n"); //$NON-NLS-1$
        builder.append("                compset -P \"$inline_prefix\" >/dev/null 2>&1\n"); //$NON-NLS-1$
        builder.append("                _mat_cli_complete_kind \"$kind\" \"$inline_prefix\"\n"); //$NON-NLS-1$
        builder.append("                return 0\n"); //$NON-NLS-1$
        builder.append("            fi\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        kind=\"$(_mat_cli_option_kind_global \"$prev\")\"\n"); //$NON-NLS-1$
        builder.append("        if _mat_cli_kind_expects_value \"$kind\"; then\n"); //$NON-NLS-1$
        builder.append("            _mat_cli_complete_kind \"$kind\" \"\"\n"); //$NON-NLS-1$
        builder.append("            return 0\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        _mat_cli_complete_words \"\" ").append(quotedWords(topLevelWords())).append('\n');
        builder.append("        return 0\n"); //$NON-NLS-1$
        builder.append("    fi\n\n"); //$NON-NLS-1$

        builder.append("    cmd=\"$reply[1]\"\n"); //$NON-NLS-1$
        builder.append("    cmd_index=\"$reply[2]\"\n"); //$NON-NLS-1$
        builder.append("    if [[ \"$cur\" == --*=* ]]; then\n"); //$NON-NLS-1$
        builder.append("        inline_option=\"${cur%%=*}\"\n"); //$NON-NLS-1$
        builder.append("        inline_prefix=\"$inline_option=\"\n"); //$NON-NLS-1$
        builder.append("        kind=\"$(_mat_cli_option_kind \"$cmd\" \"$inline_option\")\"\n"); //$NON-NLS-1$
        builder.append("        if [[ \"$kind\" == enum:* || \"$kind\" == file ]]; then\n"); //$NON-NLS-1$
        builder.append("            compset -P \"$inline_prefix\" >/dev/null 2>&1\n"); //$NON-NLS-1$
        builder.append("            _mat_cli_complete_kind \"$kind\" \"$inline_prefix\"\n"); //$NON-NLS-1$
        builder.append("            return 0\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("    fi\n"); //$NON-NLS-1$
        builder.append("    kind=\"$(_mat_cli_option_kind \"$cmd\" \"$prev\")\"\n"); //$NON-NLS-1$
        builder.append("    if _mat_cli_kind_expects_value \"$kind\"; then\n"); //$NON-NLS-1$
        builder.append("        _mat_cli_complete_kind \"$kind\" \"\"\n"); //$NON-NLS-1$
        builder.append("        return 0\n"); //$NON-NLS-1$
        builder.append("    fi\n\n"); //$NON-NLS-1$

        builder.append("    position_index=0\n"); //$NON-NLS-1$
        builder.append("    pending_kind=\"\"\n"); //$NON-NLS-1$
        builder.append("    for ((i=cmd_index+1; i<CURRENT; i++)); do\n"); //$NON-NLS-1$
        builder.append("        word=\"$words[i]\"\n"); //$NON-NLS-1$
        builder.append("        if [[ -n \"$pending_kind\" ]]; then\n"); //$NON-NLS-1$
        builder.append("            pending_kind=\"\"\n"); //$NON-NLS-1$
        builder.append("            continue\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        if [[ \"$word\" == --*=* ]]; then\n"); //$NON-NLS-1$
        builder.append("            continue\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        kind=\"$(_mat_cli_option_kind \"$cmd\" \"$word\")\"\n"); //$NON-NLS-1$
        builder.append("        if [[ -n \"$kind\" ]]; then\n"); //$NON-NLS-1$
        builder.append("            if _mat_cli_kind_expects_value \"$kind\"; then\n"); //$NON-NLS-1$
        builder.append("                pending_kind=\"$kind\"\n"); //$NON-NLS-1$
        builder.append("            fi\n"); //$NON-NLS-1$
        builder.append("            continue\n"); //$NON-NLS-1$
        builder.append("        fi\n"); //$NON-NLS-1$
        builder.append("        position_index=$((position_index + 1))\n"); //$NON-NLS-1$
        builder.append("    done\n\n"); //$NON-NLS-1$

        builder.append("    if [[ \"$cur\" == -* ]]; then\n"); //$NON-NLS-1$
        builder.append("        options_string=\"$(_mat_cli_options_for_command \"$cmd\")\"\n"); //$NON-NLS-1$
        builder.append("        options=(${=options_string})\n"); //$NON-NLS-1$
        builder.append("        _mat_cli_complete_words \"\" \"${options[@]}\"\n"); //$NON-NLS-1$
        builder.append("        return 0\n"); //$NON-NLS-1$
        builder.append("    fi\n\n"); //$NON-NLS-1$

        builder.append("    positional_kind=\"$(_mat_cli_positional_kind \"$cmd\" \"$position_index\")\"\n"); //$NON-NLS-1$
        builder.append("    case \"$positional_kind\" in\n"); //$NON-NLS-1$
        builder.append("        enum:*|file)\n"); //$NON-NLS-1$
        builder.append("            _mat_cli_complete_kind \"$positional_kind\" \"\"\n"); //$NON-NLS-1$
        builder.append("            ;;\n"); //$NON-NLS-1$
        builder.append("        *)\n"); //$NON-NLS-1$
        builder.append("            options_string=\"$(_mat_cli_options_for_command \"$cmd\")\"\n"); //$NON-NLS-1$
        builder.append("            options=(${=options_string})\n"); //$NON-NLS-1$
        builder.append("            _mat_cli_complete_words \"\" \"${options[@]}\"\n"); //$NON-NLS-1$
        builder.append("            ;;\n"); //$NON-NLS-1$
        builder.append("    esac\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$
        builder.append("compdef _mat-cli mat-cli 2>/dev/null\n"); //$NON-NLS-1$
        return builder.toString();
    }

    private void appendKindHelpers(StringBuilder builder, boolean zsh)
    {
        builder.append("_mat_cli_kind_expects_value() {\n"); //$NON-NLS-1$
        if (zsh)
            builder.append("    [[ -n \"$1\" && \"$1\" != none ]]\n"); //$NON-NLS-1$
        else
            builder.append("    [ -n \"$1\" ] && [ \"$1\" != \"none\" ]\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$
    }

    private void appendOptionKindFunctions(StringBuilder builder, boolean zsh)
    {
        appendOptionKindFunction(builder, "_mat_cli_option_kind_global", CliCommandCatalog.globalOptions()); //$NON-NLS-1$

        builder.append("_mat_cli_option_kind_for_command() {\n"); //$NON-NLS-1$
        builder.append("    case \"$1:$2\" in\n"); //$NON-NLS-1$
        for (CliCommand command : CliCommand.values())
        {
            CliCommandCatalog.CommandDefinition definition = CliCommandCatalog.lookup(command);
            if (definition == null)
                continue;
            for (CliCommandCatalog.OptionDefinition option : definition.getOptions())
            {
                builder.append("        ").append(command.getToken()).append(':').append(option.getName()).append(")\n"); //$NON-NLS-1$
                builder.append("            printf '%s\\n' '").append(completionKind(option)).append("'\n"); //$NON-NLS-1$ //$NON-NLS-2$
                builder.append("            ;;\n"); //$NON-NLS-1$
            }
        }
        builder.append("    esac\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$

        builder.append("_mat_cli_option_kind() {\n"); //$NON-NLS-1$
        builder.append("    local kind\n"); //$NON-NLS-1$
        builder.append("    kind=\"$(_mat_cli_option_kind_for_command \"$1\" \"$2\")\"\n"); //$NON-NLS-1$
        if (zsh)
        {
            builder.append("    if [[ -n \"$kind\" ]]; then\n"); //$NON-NLS-1$
            builder.append("        print -r -- \"$kind\"\n"); //$NON-NLS-1$
        }
        else
        {
            builder.append("    if [ -n \"$kind\" ]; then\n"); //$NON-NLS-1$
            builder.append("        printf '%s\\n' \"$kind\"\n"); //$NON-NLS-1$
        }
        builder.append("        return 0\n"); //$NON-NLS-1$
        builder.append("    fi\n"); //$NON-NLS-1$
        builder.append("    _mat_cli_option_kind_global \"$2\"\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$
    }

    private void appendOptionKindFunction(StringBuilder builder, String functionName,
                    List<CliCommandCatalog.OptionDefinition> options)
    {
        builder.append(functionName).append("() {\n"); //$NON-NLS-1$
        builder.append("    case \"$1\" in\n"); //$NON-NLS-1$
        for (CliCommandCatalog.OptionDefinition option : options)
        {
            builder.append("        ").append(option.getName()).append(")\n"); //$NON-NLS-1$
            builder.append("            printf '%s\\n' '").append(completionKind(option)).append("'\n"); //$NON-NLS-1$ //$NON-NLS-2$
            builder.append("            ;;\n"); //$NON-NLS-1$
        }
        builder.append("    esac\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$
    }

    private void appendOptionsForCommandFunction(StringBuilder builder, boolean zsh)
    {
        builder.append("_mat_cli_options_for_command() {\n"); //$NON-NLS-1$
        builder.append("    case \"$1\" in\n"); //$NON-NLS-1$
        for (CliCommand command : CliCommand.values())
        {
            List<String> options = new ArrayList<String>();
            for (CliCommandCatalog.OptionDefinition option : CliCommandCatalog.completionOptions(command))
            {
                options.add(option.getName());
            }
            builder.append("        ").append(command.getToken()).append(")\n"); //$NON-NLS-1$
            if (zsh)
                builder.append("            print -r -- \"").append(joinWords(options)).append("\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
            else
                builder.append("            printf '%s\\n' '").append(joinWords(options)).append("'\n"); //$NON-NLS-1$ //$NON-NLS-2$
            builder.append("            ;;\n"); //$NON-NLS-1$
        }
        builder.append("    esac\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$
    }

    private void appendPositionalKindFunction(StringBuilder builder, boolean zsh)
    {
        builder.append("_mat_cli_positional_kind() {\n"); //$NON-NLS-1$
        builder.append("    case \"$1:$2\" in\n"); //$NON-NLS-1$
        for (CliCommand command : CliCommand.values())
        {
            CliCommandCatalog.CommandDefinition definition = CliCommandCatalog.lookup(command);
            if (definition == null)
                continue;
            List<CliCommandCatalog.PositionalDefinition> positionals = definition.getPositionalDefinitions();
            for (int ii = 0; ii < positionals.size(); ii++)
            {
                builder.append("        ").append(command.getToken()).append(':').append(ii).append(")\n"); //$NON-NLS-1$
                if (zsh)
                    builder.append("            print -r -- '").append(completionKind(positionals.get(ii))).append("'\n"); //$NON-NLS-1$ //$NON-NLS-2$
                else
                    builder.append("            printf '%s\\n' '").append(completionKind(positionals.get(ii))).append("'\n"); //$NON-NLS-1$ //$NON-NLS-2$
                builder.append("            ;;\n"); //$NON-NLS-1$
            }
        }
        builder.append("    esac\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$
    }

    private List<String> topLevelWords()
    {
        List<String> words = new ArrayList<String>();
        words.addAll(CliCommandCatalog.commandTokens());
        for (CliCommandCatalog.OptionDefinition option : CliCommandCatalog.globalOptions())
        {
            words.add(option.getName());
        }
        return words;
    }

    private String completionKind(CliCommandCatalog.OptionDefinition option)
    {
        return completionKind(option.getCompletionValueType(), option.getCompletionCandidates());
    }

    private String completionKind(CliCommandCatalog.PositionalDefinition positional)
    {
        return completionKind(positional.getCompletionValueType(), positional.getCompletionCandidates());
    }

    private String completionKind(CompletionValueType type, List<String> candidates)
    {
        switch (type)
        {
            case ENUM:
                return "enum:" + joinWords(candidates); //$NON-NLS-1$
            case FILE:
                return "file"; //$NON-NLS-1$
            case FREE_TEXT:
                return "free-text"; //$NON-NLS-1$
            case NONE:
            default:
                return "none"; //$NON-NLS-1$
        }
    }

    private String joinWithPipe(List<String> values)
    {
        StringBuilder builder = new StringBuilder();
        for (int ii = 0; ii < values.size(); ii++)
        {
            if (ii > 0)
                builder.append('|');
            builder.append(values.get(ii));
        }
        return builder.toString();
    }

    private String joinWords(List<String> values)
    {
        StringBuilder builder = new StringBuilder();
        for (int ii = 0; ii < values.size(); ii++)
        {
            if (ii > 0)
                builder.append(' ');
            builder.append(values.get(ii));
        }
        return builder.toString();
    }

    private String quotedWords(List<String> values)
    {
        StringBuilder builder = new StringBuilder();
        for (String value : values)
        {
            if (builder.length() > 0)
                builder.append(' ');
            builder.append('"').append(value).append('"');
        }
        return builder.toString();
    }
}
