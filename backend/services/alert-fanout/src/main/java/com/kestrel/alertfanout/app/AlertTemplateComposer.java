package com.kestrel.alertfanout.app;

import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.common.events.AlertEventPayload;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class AlertTemplateComposer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    public record ComposedMessage(
            Language language,
            Severity severity,
            String title,
            String body,
            String actionableAdvice,
            String provenanceNote,
            String fullText
    ) {}

    public ComposedMessage composeAlert(AlertEventPayload payload, LocationReplica location, Language language) {
        Severity severity = payload.toSeverity() != null ? payload.toSeverity() : Severity.NONE;
        Provenance provenance = (location != null && location.getCurrentProvenance() != null)
                ? location.getCurrentProvenance()
                : (payload.provenance() != null ? payload.provenance() : Provenance.MODEL_ONLY);
        return composeAlert(location, severity, provenance, payload.firstExceedanceDate(), language);
    }

    public ComposedMessage composeAlert(LocationReplica location, Severity severity, Provenance provenance, LocalDate peakDate, Language language) {
        String locationName = (location != null && location.getName() != null) ? location.getName() : "Unknown Location";
        if (provenance == null) {
            provenance = (location != null && location.getCurrentProvenance() != null) ? location.getCurrentProvenance() : Provenance.MODEL_ONLY;
        }
        String peakDateStr = formatPeakDate(peakDate, language);
        String provLabel = getProvenanceLabel(provenance, language);

        return switch (language) {
            case hi -> composeHindi(severity, locationName, peakDateStr, provLabel);
            case mr -> composeMarathi(severity, locationName, peakDateStr, provLabel);
            case en -> composeEnglish(severity, locationName, peakDateStr, provLabel);
        };
    }

    public String composeWhatsAppText(AlertEventPayload payload, LocationReplica location) {
        Severity severity = payload.toSeverity() != null ? payload.toSeverity() : Severity.NONE;
        Provenance provenance = (location != null && location.getCurrentProvenance() != null)
                ? location.getCurrentProvenance()
                : (payload.provenance() != null ? payload.provenance() : Provenance.MODEL_ONLY);
        return composeWhatsAppText(location, severity, provenance, payload.firstExceedanceDate(), null);
    }

    public String composeWhatsAppText(LocationReplica location, Severity severity, Provenance provenance, LocalDate peakDate, String customNote) {
        Language regionalLang = (location != null && location.getPrimaryLanguage() != null)
                ? location.getPrimaryLanguage()
                : Language.mr;

        ComposedMessage english = composeAlert(location, severity, provenance, peakDate, Language.en);
        ComposedMessage regional = composeAlert(location, severity, provenance, peakDate, regionalLang);

        String headerIcon = switch (severity != null ? severity : Severity.NONE) {
            case SEVERE -> "🚨";
            case WARNING -> "⚠️";
            case WATCH -> "👁️";
            case NONE -> "✅";
        };

        StringBuilder sb = new StringBuilder();
        sb.append(headerIcon).append(" KESTREL FLOOD ALERT");
        if (regionalLang == Language.mr) {
            sb.append(" / पूर इशारा");
        } else if (regionalLang == Language.hi) {
            sb.append(" / बाढ़ चेतावनी");
        }
        sb.append("\n");

        sb.append("📍 Location: ").append(location != null ? location.getName() : "Unknown Location").append("\n\n");

        sb.append("English:\n")
                .append(english.body()).append(" ")
                .append(english.actionableAdvice());
        if (customNote != null && !customNote.isBlank()) {
            sb.append("\nNote: ").append(customNote.trim());
        }
        sb.append("\n\n");

        if (regionalLang != Language.en) {
            String langHeader = (regionalLang == Language.mr) ? "मराठी:" : "हिंदी:";
            sb.append(langHeader).append("\n")
                    .append(regional.body()).append(" ")
                    .append(regional.actionableAdvice());
            if (customNote != null && !customNote.isBlank()) {
                sb.append("\nसूचना: ").append(customNote.trim());
            }
            sb.append("\n\n");
        }

        sb.append("ℹ️ Provenance: ").append(english.provenanceNote());

        return sb.toString().trim();
    }

    public String generateWhatsAppShareUrl(AlertEventPayload payload, LocationReplica location) {
        String text = composeWhatsAppText(payload, location);
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
        return "https://wa.me/?text=" + encoded;
    }

    public String generateWhatsAppShareUrl(LocationReplica location, Severity severity, Provenance provenance, LocalDate peakDate, String customNote) {
        String text = composeWhatsAppText(location, severity, provenance, peakDate, customNote);
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
        return "https://wa.me/?text=" + encoded;
    }

    public String getProvenanceLabel(Provenance provenance, Language language) {
        if (provenance == null) {
            provenance = Provenance.MODEL_ONLY;
        }
        return switch (language) {
            case hi -> switch (provenance) {
                case MODEL_ONLY -> "केवल मॉडल अनुमान";
                case MODEL_PLUS_REPORTS -> "मॉडल अनुमान + स्थानीय जन रिपोर्ट";
                case OFFICIAL_CONFIRMED -> "आधिकारिक पुष्टि सहित";
            };
            case mr -> switch (provenance) {
                case MODEL_ONLY -> "केवळ मॉडेल अंदाज";
                case MODEL_PLUS_REPORTS -> "मॉडेल अंदाज + स्थानिक नागरिकांचे अहवाल";
                case OFFICIAL_CONFIRMED -> "अधिकृत पुष्टीसह";
            };
            case en -> switch (provenance) {
                case MODEL_ONLY -> "Model Guidance Only";
                case MODEL_PLUS_REPORTS -> "Model Guidance + Community Reports";
                case OFFICIAL_CONFIRMED -> "Official Agency Corroborated";
            };
        };
    }

    private String formatPeakDate(LocalDate date, Language language) {
        if (date == null) {
            return switch (language) {
                case hi -> "अगले 48 घंटे";
                case mr -> "पुढील ४८ तास";
                case en -> "next 48 hours";
            };
        }
        return date.format(DATE_FMT);
    }

    private record TemplateFields(String title, String body, String advice) {}

    private ComposedMessage composeEnglish(Severity severity, String locationName, String peakDate, String provLabel) {
        String provNote = provLabel + " — Not an official warning. Confirm locally.";

        TemplateFields f = switch (severity) {
            case SEVERE -> new TemplateFields(
                    "🚨 KESTREL SEVERE FLOOD EMERGENCY: " + locationName,
                    "Catastrophic river discharge forecast within the next 24 to 48 hours (Peak expected on " + peakDate + ").",
                    "Action: Evacuate low-lying areas immediately. Seek high ground. Follow local authority instructions."
            );
            case WARNING -> new TemplateFields(
                    "⚠️ KESTREL FLOOD WARNING: " + locationName,
                    "River discharge is forecast to exceed dangerous levels within the next 48 hours (Peak expected on " + peakDate + ").",
                    "Action: Move livestock and valuables to upper floors. Avoid walking or driving near river embankments."
            );
            case WATCH -> new TemplateFields(
                    "👁️ KESTREL FLOOD WATCH: " + locationName,
                    "River discharge is forecast to reach watchful levels within the next 72 hours (Expected on " + peakDate + ").",
                    "Action: Monitor local river conditions and keep emergency supplies ready."
            );
            case NONE -> new TemplateFields(
                    "✅ KESTREL FLOOD ALL CLEAR: " + locationName,
                    "River discharge has receded below alert thresholds. Normal conditions have resumed.",
                    "Action: Normal activities may resume. Stay informed of weather forecasts."
            );
        };

        String fullText = f.title() + "\n" + f.body() + "\n" + f.advice() + "\nProvenance: " + provNote;
        return new ComposedMessage(Language.en, severity, f.title(), f.body(), f.advice(), provNote, fullText);
    }

    private ComposedMessage composeHindi(Severity severity, String locationName, String peakDate, String provLabel) {
        String provNote = provLabel + " — यह आधिकारिक चेतावनी नहीं है। स्थानीय प्रशासन के निर्देशों का पालन करें।";

        TemplateFields f = switch (severity) {
            case SEVERE -> new TemplateFields(
                    "🚨 केस्ट्रल गंभीर बाढ़ आपातकाल: " + locationName,
                    "अगले 24 से 48 घंटों में विनाशकारी जलस्तर का अनुमान है (अधिकतम बहाव: " + peakDate + ")।",
                    "सुरक्षा निर्देश: निचले इलाकों को तुरंत खाली करें। ऊंचे स्थानों पर जाएं। स्थानीय प्रशासन के निर्देशों का पालन करें।"
            );
            case WARNING -> new TemplateFields(
                    "⚠️ केस्ट्रल बाढ़ चेतावनी: " + locationName,
                    "अगले 48 घंटों में नदी का जलस्तर खतरनाक सीमा को पार करने का अनुमान है (अधिकतम बहाव: " + peakDate + ")।",
                    "सुरक्षा निर्देश: पशुओं और कीमती सामान को ऊंचे स्थानों पर पहुंचाएं। नदी तटों और जलभराव वाले रास्तों पर जाने से बचें।"
            );
            case WATCH -> new TemplateFields(
                    "👁️ केस्ट्रल बाढ़ निगरानी: " + locationName,
                    "अगले 72 घंटों में नदी का जलस्तर निगरानी स्तर तक पहुंचने का अनुमान है (संभावित तिथि: " + peakDate + ")।",
                    "सुरक्षा निर्देश: नदी की स्थिति पर नज़र रखें और आपातकालीन सामग्री तैयार रखें।"
            );
            case NONE -> new TemplateFields(
                    "✅ केस्ट्रल बाढ़ सामान्य स्थिति: " + locationName,
                    "नदी का जलस्तर चेतावनी सीमा से नीचे आ गया है। स्थिति सामान्य है।",
                    "सुरक्षा निर्देश: सामान्य गतिविधियां पुनः प्रारंभ की जा सकती हैं। मौसम के पूर्वानुमानों से अवगत रहें।"
            );
        };

        String fullText = f.title() + "\n" + f.body() + "\n" + f.advice() + "\nस्रोत: " + provNote;
        return new ComposedMessage(Language.hi, severity, f.title(), f.body(), f.advice(), provNote, fullText);
    }

    private ComposedMessage composeMarathi(Severity severity, String locationName, String peakDate, String provLabel) {
        String provNote = provLabel + " — हा अधिकृत इशारा नाही. स्थानिक पातळीवर खात्री करा.";

        TemplateFields f = switch (severity) {
            case SEVERE -> new TemplateFields(
                    "🚨 केस्ट्रल अतिगंभीर पूर आणीबाणी: " + locationName,
                    "पुढील २४ ते ४८ तासांत अतिधोकादायक पूरस्थिती उद्भवण्याची शक्यता आहे (सर्वोच्च पातळी: " + peakDate + ").",
                    "सुरक्षा सूचना: सखल भागातील जागा त्वरित रिकामी करा. उंच सुरक्षित ठिकाणी जा. आपत्कालीन यंत्रणेच्या सूचनांचे पालन करा."
            );
            case WARNING -> new TemplateFields(
                    "⚠️ केस्ट्रल पूर इशारा: " + locationName,
                    "पुढील ४८ तासांत नदीची पाणीपातळी धोकादायक पातळी ओलांडण्याची शक्यता आहे (अपेक्षित सर्वोच्च पातळी: " + peakDate + ").",
                    "सुरक्षा सूचना: जनावरे व महत्त्वाची सामग्री सुरक्षित किंवा उंच जागी हलवा. नदीकाठच्या भागात जाणे टाळा."
            );
            case WATCH -> new TemplateFields(
                    "👁️ केस्ट्रल पूर पूर्वसूचना: " + locationName,
                    "पुढील ७२ तासांत नदीची पाणीपातळी वाढण्याची शक्यता आहे (अपेक्षित तारीख: " + peakDate + ").",
                    "सुरक्षा सूचना: स्थानिक नदीच्या स्थितीवर लक्ष ठेवा आणि आवश्यक साहित्य सज्ज ठेवा."
            );
            case NONE -> new TemplateFields(
                    "✅ केस्ट्रल पूर स्थिती पूर्ववत: " + locationName,
                    "नदीची पाणीपातळी इशारा पातळीच्या खाली आली आहे. परिस्थिती पूर्ववत झाली आहे.",
                    "सुरक्षा सूचना: दैनंदिन कामकाज पूर्ववत सुरू करता येईल. हवामानाच्या अंदाजांकडे लक्ष ठेवा."
            );
        };

        String fullText = f.title() + "\n" + f.body() + "\n" + f.advice() + "\nमाहिती स्रोत: " + provNote;
        return new ComposedMessage(Language.mr, severity, f.title(), f.body(), f.advice(), provNote, fullText);
    }
}
