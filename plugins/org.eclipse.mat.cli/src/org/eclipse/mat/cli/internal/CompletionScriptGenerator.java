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
    private static final String BASH_TEMPLATE = """
# bash completion for mat-cli
_mat_cli_is_command() {
    case "$1" in
__COMMAND_CASE__            return 0
            ;;
    esac
    return 1
}

__KIND_HELPERS____OPTION_KIND_FUNCTIONS____OPTIONS_FOR_COMMAND_FUNCTION____POSITIONAL_KIND_FUNCTION___mat_cli_set_word_replies() {
    local prefix="$1"
    local current="$2"
    local values="$3"
    local i
    COMPREPLY=( $(compgen -W "$values" -- "$current") )
    if [ -n "$prefix" ]; then
        for ((i=0; i<${#COMPREPLY[@]}; i++)); do
            COMPREPLY[i]="$prefix${COMPREPLY[i]}"
        done
    fi
}

_mat_cli_set_file_replies() {
    local prefix="$1"
    local current="$2"
    local line
    COMPREPLY=()
    while IFS= read -r line; do
        if [ -n "$prefix" ]; then
            COMPREPLY+=("$prefix$line")
        else
            COMPREPLY+=("$line")
        fi
    done < <(compgen -f -- "$current")
    if type compopt >/dev/null 2>&1; then
        compopt -o filenames 2>/dev/null
    fi
}

_mat_cli_complete_kind() {
    local kind="$1"
    local current="$2"
    local prefix="$3"
    case "$kind" in
        enum:*)
            _mat_cli_set_word_replies "$prefix" "$current" "${kind#enum:}"
            ;;
        file)
            _mat_cli_set_file_replies "$prefix" "$current"
            ;;
        *)
            COMPREPLY=()
            ;;
    esac
}

_mat_cli_find_command_index() {
    local i word pending_kind=""
    for ((i=1; i<COMP_CWORD; i++)); do
        word="${COMP_WORDS[i]}"
        if [ -n "$pending_kind" ]; then
            pending_kind=""
            continue
        fi
        if _mat_cli_is_command "$word"; then
            printf '%s\\n' "$i"
            return 0
        fi
        if [[ "$word" == --*=* ]]; then
            continue
        fi
        pending_kind="$(_mat_cli_option_kind_global "$word")"
        if ! _mat_cli_kind_expects_value "$pending_kind"; then
            pending_kind=""
        fi
    done
}

_mat_cli_completion() {
    local cur prev cmd_index cmd position_index pending_kind inline_option inline_prefix kind options
    local i word positional_kind
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    cmd_index="$(_mat_cli_find_command_index)"

    if [ -z "$cmd_index" ]; then
        if [[ "$cur" == --*=* ]]; then
            inline_option="${cur%%=*}"
            inline_prefix="$inline_option="
            kind="$(_mat_cli_option_kind_global "$inline_option")"
            if _mat_cli_kind_expects_value "$kind"; then
                _mat_cli_complete_kind "$kind" "${cur#*=}" "$inline_prefix"
                return 0
            fi
        fi
        kind="$(_mat_cli_option_kind_global "$prev")"
        if _mat_cli_kind_expects_value "$kind"; then
            _mat_cli_complete_kind "$kind" "$cur" ""
            return 0
        fi
        _mat_cli_set_word_replies "" "$cur" "__TOP_LEVEL_WORDS__"
        return 0
    fi

    cmd="${COMP_WORDS[cmd_index]}"
    if [[ "$cur" == --*=* ]]; then
        inline_option="${cur%%=*}"
        inline_prefix="$inline_option="
        kind="$(_mat_cli_option_kind "$cmd" "$inline_option")"
        if _mat_cli_kind_expects_value "$kind"; then
            _mat_cli_complete_kind "$kind" "${cur#*=}" "$inline_prefix"
            return 0
        fi
    fi
    kind="$(_mat_cli_option_kind "$cmd" "$prev")"
    if _mat_cli_kind_expects_value "$kind"; then
        _mat_cli_complete_kind "$kind" "$cur" ""
        return 0
    fi

    position_index=0
    pending_kind=""
    for ((i=cmd_index+1; i<COMP_CWORD; i++)); do
        word="${COMP_WORDS[i]}"
        if [ -n "$pending_kind" ]; then
            pending_kind=""
            continue
        fi
        if [[ "$word" == --*=* ]]; then
            continue
        fi
        kind="$(_mat_cli_option_kind "$cmd" "$word")"
        if [ -n "$kind" ]; then
            if _mat_cli_kind_expects_value "$kind"; then
                pending_kind="$kind"
            fi
            continue
        fi
        position_index=$((position_index + 1))
    done

    if [[ "$cur" == -* ]]; then
        options="$(_mat_cli_options_for_command "$cmd")"
        _mat_cli_set_word_replies "" "$cur" "$options"
        return 0
    fi

    positional_kind="$(_mat_cli_positional_kind "$cmd" "$position_index")"
    case "$positional_kind" in
        enum:*|file)
            _mat_cli_complete_kind "$positional_kind" "$cur" ""
            ;;
        *)
            options="$(_mat_cli_options_for_command "$cmd")"
            _mat_cli_set_word_replies "" "$cur" "$options"
            ;;
    esac
}

complete -F _mat_cli_completion mat-cli
"""; //$NON-NLS-1$

    private static final String ZSH_TEMPLATE = """
#compdef mat-cli
# zsh completion for mat-cli
_mat_cli_is_command() {
    case "$1" in
__COMMAND_CASE__            return 0
            ;;
    esac
    return 1
}

__KIND_HELPERS____OPTION_KIND_FUNCTIONS____OPTIONS_FOR_COMMAND_FUNCTION____POSITIONAL_KIND_FUNCTION___mat_cli_complete_words() {
    local prefix="$1"
    shift
    if [[ -n "$prefix" ]]; then
        compadd -Q -P "$prefix" -- "$@"
    else
        compadd -Q -- "$@"
    fi
}

_mat_cli_complete_kind() {
    local kind="$1"
    local prefix="$2"
    local values_string
    local -a values
    case "$kind" in
        enum:*)
            values_string="${kind#enum:}"
            values=(${=values_string})
            _mat_cli_complete_words "$prefix" "${values[@]}"
            ;;
        file)
            if [[ -n "$prefix" ]]; then
                compset -P "$prefix" >/dev/null 2>&1
                _files -P "$prefix"
            else
                _files
            fi
            ;;
        *)
            return 1
            ;;
    esac
}

_mat_cli_find_command() {
    local i word pending_kind=""
    reply=()
    for ((i=2; i<CURRENT; i++)); do
        word="$words[i]"
        if [[ -n "$pending_kind" ]]; then
            pending_kind=""
            continue
        fi
        if _mat_cli_is_command "$word"; then
            reply=("$word" "$i")
            return 0
        fi
        if [[ "$word" == --*=* ]]; then
            continue
        fi
        pending_kind="$(_mat_cli_option_kind_global "$word")"
        if ! _mat_cli_kind_expects_value "$pending_kind"; then
            pending_kind=""
        fi
    done
    return 1
}

_mat-cli() {
    local cur prev cmd cmd_index position_index pending_kind inline_option inline_prefix kind positional_kind options_string
    local i word values_string
    local -a values matches options
    cur="$words[CURRENT]"
    prev="$words[CURRENT-1]"

    if ! _mat_cli_find_command; then
        if [[ "$cur" == --*=* ]]; then
            inline_option="${cur%%=*}"
            inline_prefix="$inline_option="
            kind="$(_mat_cli_option_kind_global "$inline_option")"
            if [[ "$kind" == enum:* ]]; then
                compset -P "$inline_prefix" >/dev/null 2>&1
                _mat_cli_complete_kind "$kind" "$inline_prefix"
                return 0
            fi
        fi
        kind="$(_mat_cli_option_kind_global "$prev")"
        if _mat_cli_kind_expects_value "$kind"; then
            _mat_cli_complete_kind "$kind" ""
            return 0
        fi
        _mat_cli_complete_words "" __TOP_LEVEL_WORDS__
        return 0
    fi

    cmd="$reply[1]"
    cmd_index="$reply[2]"
    if [[ "$cur" == --*=* ]]; then
        inline_option="${cur%%=*}"
        inline_prefix="$inline_option="
        kind="$(_mat_cli_option_kind "$cmd" "$inline_option")"
        if [[ "$kind" == enum:* || "$kind" == file ]]; then
            compset -P "$inline_prefix" >/dev/null 2>&1
            _mat_cli_complete_kind "$kind" "$inline_prefix"
            return 0
        fi
    fi
    kind="$(_mat_cli_option_kind "$cmd" "$prev")"
    if _mat_cli_kind_expects_value "$kind"; then
        _mat_cli_complete_kind "$kind" ""
        return 0
    fi

    position_index=0
    pending_kind=""
    for ((i=cmd_index+1; i<CURRENT; i++)); do
        word="$words[i]"
        if [[ -n "$pending_kind" ]]; then
            pending_kind=""
            continue
        fi
        if [[ "$word" == --*=* ]]; then
            continue
        fi
        kind="$(_mat_cli_option_kind "$cmd" "$word")"
        if [[ -n "$kind" ]]; then
            if _mat_cli_kind_expects_value "$kind"; then
                pending_kind="$kind"
            fi
            continue
        fi
        position_index=$((position_index + 1))
    done

    if [[ "$cur" == -* ]]; then
        options_string="$(_mat_cli_options_for_command "$cmd")"
        options=(${=options_string})
        _mat_cli_complete_words "" "${options[@]}"
        return 0
    fi

    positional_kind="$(_mat_cli_positional_kind "$cmd" "$position_index")"
    case "$positional_kind" in
        enum:*|file)
            _mat_cli_complete_kind "$positional_kind" ""
            ;;
        *)
            options_string="$(_mat_cli_options_for_command "$cmd")"
            options=(${=options_string})
            _mat_cli_complete_words "" "${options[@]}"
            ;;
    esac
}

compdef _mat-cli mat-cli 2>/dev/null
"""; //$NON-NLS-1$

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
        return BASH_TEMPLATE.replace("__COMMAND_CASE__", renderCommandCase()) //$NON-NLS-1$ //$NON-NLS-2$
                        .replace("__KIND_HELPERS__", renderKindHelpers(false)) //$NON-NLS-1$
                        .replace("__OPTION_KIND_FUNCTIONS__", renderOptionKindFunctions(false)) //$NON-NLS-1$
                        .replace("__OPTIONS_FOR_COMMAND_FUNCTION__", renderOptionsForCommandFunction(false)) //$NON-NLS-1$
                        .replace("__POSITIONAL_KIND_FUNCTION__", renderPositionalKindFunction(false)) //$NON-NLS-1$
                        .replace("__TOP_LEVEL_WORDS__", joinWords(topLevelWords())); //$NON-NLS-1$
    }

    private String generateZsh()
    {
        return ZSH_TEMPLATE.replace("__COMMAND_CASE__", renderCommandCase()) //$NON-NLS-1$ //$NON-NLS-2$
                        .replace("__KIND_HELPERS__", renderKindHelpers(true)) //$NON-NLS-1$
                        .replace("__OPTION_KIND_FUNCTIONS__", renderOptionKindFunctions(true)) //$NON-NLS-1$
                        .replace("__OPTIONS_FOR_COMMAND_FUNCTION__", renderOptionsForCommandFunction(true)) //$NON-NLS-1$
                        .replace("__POSITIONAL_KIND_FUNCTION__", renderPositionalKindFunction(true)) //$NON-NLS-1$
                        .replace("__TOP_LEVEL_WORDS__", quotedWords(topLevelWords())); //$NON-NLS-1$
    }

    private String renderCommandCase()
    {
        return "        " + joinWithPipe(CliCommandCatalog.commandTokens()) + ")\n"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String renderKindHelpers(boolean zsh)
    {
        StringBuilder builder = new StringBuilder(64);
        builder.append("_mat_cli_kind_expects_value() {\n"); //$NON-NLS-1$
        if (zsh)
            builder.append("    [[ -n \"$1\" && \"$1\" != none ]]\n"); //$NON-NLS-1$
        else
            builder.append("    [ -n \"$1\" ] && [ \"$1\" != \"none\" ]\n"); //$NON-NLS-1$
        builder.append("}\n\n"); //$NON-NLS-1$
        return builder.toString();
    }

    private String renderOptionKindFunctions(boolean zsh)
    {
        StringBuilder builder = new StringBuilder(4096);
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
        return builder.toString();
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

    private String renderOptionsForCommandFunction(boolean zsh)
    {
        StringBuilder builder = new StringBuilder(2048);
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
        return builder.toString();
    }

    private String renderPositionalKindFunction(boolean zsh)
    {
        StringBuilder builder = new StringBuilder(2048);
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
        return builder.toString();
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
