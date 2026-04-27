package com.mrigl.donationapp.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class TextToSpeechService {

    private final AtomicReference<Process> currentSpeechProcess = new AtomicReference<>();

    public boolean isSupported() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    public void speakAsync(String text) {
        if (!isSupported() || text == null || text.isBlank()) {
            return;
        }
        stop();

        String lang = detectLanguageCode(text);
        String payload = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        String langPayload = Base64.getEncoder().encodeToString(lang.getBytes(StandardCharsets.UTF_8));
        String script = "$t=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('" + payload + "'));"
                + "$lang=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('" + langPayload + "'));"
                + "Add-Type -AssemblyName System.Speech;"
                + "$s=New-Object System.Speech.Synthesis.SpeechSynthesizer;"
                + "$v=$null;"
                + "if($lang -eq 'ar'){ $v=$s.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -like 'ar*' } | Select-Object -First 1 };"
                + "if($lang -eq 'en'){ $v=$s.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -like 'en*' } | Select-Object -First 1 };"
                + "if($lang -eq 'fr'){ $v=$s.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -like 'fr*' } | Select-Object -First 1 };"
                + "if($v -ne $null){ $s.SelectVoice($v.VoiceInfo.Name) };"
                + "$s.Rate=0;"
                + "$s.Speak($t);";

        Thread t = new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "powershell",
                        "-NoProfile",
                        "-NonInteractive",
                        "-Command",
                        script
                );
                pb.redirectErrorStream(true);
                Process p = pb.start();
                currentSpeechProcess.set(p);
                p.waitFor();
            } catch (Exception ignored) {
                // Keep UI flow uninterrupted if TTS fails.
            } finally {
                currentSpeechProcess.set(null);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        Process running = currentSpeechProcess.getAndSet(null);
        if (running != null && running.isAlive()) {
            running.destroyForcibly();
        }
    }

    private String detectLanguageCode(String text) {
        if (text == null || text.isBlank()) {
            return "fr";
        }
        String lower = text.toLowerCase(Locale.ROOT);

        int arabicChars = 0;
        int latinChars = 0;
        for (char c : lower.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ARABIC) {
                arabicChars++;
            } else if (c >= 'a' && c <= 'z') {
                latinChars++;
            }
        }
        if (arabicChars > 0 && arabicChars >= latinChars / 2) {
            return "ar";
        }

        int frHints = count(lower, " le ") + count(lower, " la ") + count(lower, " les ")
                + count(lower, " de ") + count(lower, " des ") + count(lower, " et ")
                + count(lower, " annonce ") + count(lower, " don ") + count(lower, " urgence ");
        int enHints = count(lower, " the ") + count(lower, " and ") + count(lower, " donation ")
                + count(lower, " urgent ") + count(lower, " need ") + count(lower, " please ");
        if (frHints >= enHints) {
            return "fr";
        }
        return "en";
    }

    private int count(String text, String needle) {
        int idx = 0;
        int c = 0;
        while ((idx = text.indexOf(needle, idx)) >= 0) {
            c++;
            idx += needle.length();
        }
        return c;
    }
}
