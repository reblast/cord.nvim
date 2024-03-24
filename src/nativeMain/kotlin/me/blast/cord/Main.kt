@file:Suppress("unused")

package me.blast.cord

import io.github.reblast.kpresence.RichClient
import io.github.reblast.kpresence.rpc.*
import io.github.reblast.kpresence.utils.*
import kotlinx.coroutines.*
import me.blast.cord.mappings.*

private const val GITHUB_ASSETS_URL =
    "https://raw.githubusercontent.com/reblast/cord.nvim/master/assets"
private var client: RichClient? = null
private var editor = "neovim"

// Presence data
private var cwd = ""
private var presenceStartTime = epochMillis()
private lateinit var presenceSmallText: String
private lateinit var idleText: String
private lateinit var viewingText: String
private lateinit var editingText: String
private lateinit var fileBrowserText: String
private lateinit var pluginManagerText: String
private lateinit var workspaceText: String

@CName("init")
fun init(
    _editor: String,
    _presenceSmallText: String,
    _idleText: String,
    _viewingText: String,
    _editingText: String,
    _fileBrowserText: String,
    _pluginManagerText: String,
    _workspaceText: String
): String? {
  return try {
    client =
        when (_editor) {
          "vim" -> RichClient(1219918645770059796)
          "neovim" -> RichClient(1219918880005165137)
          "lunarvim" -> RichClient(1220295374087000104)
          "nvchad" -> RichClient(1220296082861326378)
          else -> RichClient(_editor.toLong())
        }.connect()
    editor = _editor
    presenceSmallText = _presenceSmallText
    idleText = _idleText
    viewingText = _viewingText
    editingText = _editingText
    fileBrowserText = _fileBrowserText
    pluginManagerText = _pluginManagerText
    workspaceText = _workspaceText

    null
  } catch (e: Exception) {
    e.message
  }
}

@CName("update_presence")
fun updatePresence(filename: String, filetype: String, isReadOnly: Boolean): String? {
  return try {
    client?.connect {
      var presenceDetails: String
      var presenceLargeImage: String
      var presenceLargeText: String

      when (filetype) {
        "cord.idle" -> {
          presenceDetails = idleText
          presenceLargeImage = "$GITHUB_ASSETS_URL/editor/idle.png"
          presenceLargeText = "💤"
        }
        "netrw", "dirvish", "TelescopePrompt" -> {
          val fileBrowser = fileBrowsers[filetype] ?: return@connect

          presenceDetails = fileBrowserText.replaceFirst("\$s", fileBrowser.second)
          presenceLargeImage = "$GITHUB_ASSETS_URL/file_browser/${fileBrowser.first}.png"
          presenceLargeText = fileBrowser.second
        }
        "lazy", "packer" -> {
          val pluginManager = pluginManagers[filetype] ?: return@connect

          presenceDetails = pluginManagerText.replaceFirst("\$s", pluginManager.second)
          presenceLargeImage = "$GITHUB_ASSETS_URL/plugin_manager/${pluginManager.first}.png"
          presenceLargeText = pluginManager.second
        }
        else -> {
          if (filename.isBlank()) {
            if (!filetype.isBlank()) return@connect

            presenceDetails =
                (if (isReadOnly) viewingText else editingText).replaceFirst("\$s", "a new file")
            presenceLargeImage = "$GITHUB_ASSETS_URL/language/text.png"
            presenceLargeText = "New buffer"
          } else {
            val language = languages[filetype] ?: ("text" to filetype)

            presenceDetails =
                (if (isReadOnly) viewingText else editingText).replaceFirst("\$s", filename)
            presenceLargeImage = "$GITHUB_ASSETS_URL/language/${language.first}.png"
            presenceLargeText = language.second
          }
        }
      }

      update(
          activity {
            details = presenceDetails
            if (cwd.isNotBlank() && workspaceText.isNotBlank()) {
              state = workspaceText.replaceFirst("\$s", cwd)
            }

            assets {
              largeImage = presenceLargeImage
              largeText = presenceLargeText
              smallImage = "$GITHUB_ASSETS_URL/editor/$editor.png"
              smallText = presenceSmallText
            }

            timestamps { start = presenceStartTime }
          }
      )
    }

    null
  } catch (e: Exception) {
    e.message
  }
}

@CName("disconnect")
fun disconnect() {
  try {
    if (client != null) {
      client!!.shutdown()
      client = null
    }
  } catch (_: Exception) {}
}

@CName("set_cwd")
fun setCwd(value: String) {
  cwd = value
}