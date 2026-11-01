package com.kestrel.alertfanout.app;

import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.common.events.AlertEventPayload;
import com.kestrel.common.events.Hazard;
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
        Hazard hazard = (payload != null && payload.hazard() != null) ? payload.hazard() : Hazard.FLOOD;
        Severity severity = (payload != null && payload.toSeverity() != null) ? payload.toSeverity() : Severity.NONE;
        Provenance provenance = (location != null && location.getCurrentProvenance() != null)
                ? location.getCurrentProvenance()
                : (payload != null && payload.provenance() != null ? payload.provenance() : Provenance.MODEL_ONLY);
        LocalDate peakDate = payload != null ? payload.firstExceedanceDate() : null;
        return composeAlert(hazard, location, severity, provenance, peakDate, language);
    }

    public ComposedMessage composeAlert(LocationReplica location, Severity severity, Provenance provenance, LocalDate peakDate, Language language) {
        return composeAlert(Hazard.FLOOD, location, severity, provenance, peakDate, language);
    }

    public ComposedMessage composeAlert(Hazard hazard, LocationReplica location, Severity severity, Provenance provenance, LocalDate peakDate, Language language) {
        if (hazard == null) {
            hazard = Hazard.FLOOD;
        }
        if (severity == null) {
            severity = Severity.NONE;
        }
        String locationName = (location != null && location.getName() != null) ? location.getName() : "Unknown Location";
        if (provenance == null) {
            provenance = (location != null && location.getCurrentProvenance() != null) ? location.getCurrentProvenance() : Provenance.MODEL_ONLY;
        }
        String peakDateStr = formatPeakDate(peakDate, language);
        String provLabel = getProvenanceLabel(provenance, language);

        return switch (language) {
            case hi -> composeHindi(hazard, severity, locationName, peakDateStr, provLabel, provenance);
            case mr -> composeMarathi(hazard, severity, locationName, peakDateStr, provLabel, provenance);
            case en -> composeEnglish(hazard, severity, locationName, peakDateStr, provLabel, provenance);
        };
    }

    public String composeWhatsAppText(AlertEventPayload payload, LocationReplica location) {
        Hazard hazard = (payload != null && payload.hazard() != null) ? payload.hazard() : Hazard.FLOOD;
        Severity severity = (payload != null && payload.toSeverity() != null) ? payload.toSeverity() : Severity.NONE;
        Provenance provenance = (location != null && location.getCurrentProvenance() != null)
                ? location.getCurrentProvenance()
                : (payload != null && payload.provenance() != null ? payload.provenance() : Provenance.MODEL_ONLY);
        return composeWhatsAppText(hazard, location, severity, provenance, payload != null ? payload.firstExceedanceDate() : null, null);
    }

    public String composeWhatsAppText(LocationReplica location, Severity severity, Provenance provenance, LocalDate peakDate, String customNote) {
        return composeWhatsAppText(Hazard.FLOOD, location, severity, provenance, peakDate, customNote);
    }

    public String composeWhatsAppText(Hazard hazard, LocationReplica location, Severity severity, Provenance provenance, LocalDate peakDate, String customNote) {
        if (hazard == null) {
            hazard = Hazard.FLOOD;
        }
        Language regionalLang = (location != null && location.getPrimaryLanguage() != null)
                ? location.getPrimaryLanguage()
                : Language.mr;

        ComposedMessage english = composeAlert(hazard, location, severity, provenance, peakDate, Language.en);
        ComposedMessage regional = composeAlert(hazard, location, severity, provenance, peakDate, regionalLang);

        String headerIcon = switch (severity != null ? severity : Severity.NONE) {
            case SEVERE -> "🚨";
            case WARNING -> "⚠️";
            case WATCH -> "👁️";
            case NONE -> "✅";
        };

        StringBuilder sb = new StringBuilder();
        String hazardEn = switch (hazard) {
            case FLOOD -> "FLOOD ALERT";
            case HEAT -> "HEAT WAVE ALERT";
            case AIR_QUALITY -> "AIR QUALITY ALERT";
        };
        String hazardRegional = switch (regionalLang) {
            case hi -> switch (hazard) {
                case FLOOD -> "बाढ़ चेतावनी";
                case HEAT -> "लू चेतावनी";
                case AIR_QUALITY -> "वायु गुणवत्ता चेतावनी";
            };
            case mr -> switch (hazard) {
                case FLOOD -> "पूर इशारा";
                case HEAT -> "उष्णतेची लाट इशारा";
                case AIR_QUALITY -> "हवा गुणवत्ता इशारा";
            };
            case en -> "";
        };

        sb.append(headerIcon).append(" KESTREL ").append(hazardEn);
        if (regionalLang != Language.en && !hazardRegional.isEmpty()) {
            sb.append(" / ").append(hazardRegional);
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
        return generateWhatsAppShareUrl(Hazard.FLOOD, location, severity, provenance, peakDate, customNote);
    }

    public String generateWhatsAppShareUrl(Hazard hazard, LocationReplica location, Severity severity, Provenance provenance, LocalDate peakDate, String customNote) {
        String text = composeWhatsAppText(hazard, location, severity, provenance, peakDate, customNote);
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

    public String getProvenanceStatement(Provenance provenance, Language language) {
        if (provenance == null) {
            provenance = Provenance.MODEL_ONLY;
        }
        return switch (language) {
            case hi -> switch (provenance) {
                case MODEL_ONLY -> "संख्यात्मक मॉडल पूर्वानुमान पर आधारित मार्गदर्शन। आपातकालीन कार्रवाई करने से पहले कृपया स्थानीय स्तर पर पुष्टि करें। यह आधिकारिक सरकारी आदेश नहीं है।";
                case MODEL_PLUS_REPORTS -> "स्थानीय सामुदायिक धरातलीय रिपोर्टों द्वारा समर्थित मॉडल मार्गदर्शन।";
                case OFFICIAL_CONFIRMED -> "सरकारी आपदा प्रबंधन प्राधिकरण द्वारा आधिकारिक रूप से पुष्ट।";
            };
            case mr -> switch (provenance) {
                case MODEL_ONLY -> "संख्यात्मक मॉडेल अंदाजावर आधारित मार्गदर्शन. कोणतीही आणीबाणीची कृती करण्यापूर्वी कृपया स्थानिक पातळीवर खात्री करा. हा अधिकृत सरकारी आदेश नाही.";
                case MODEL_PLUS_REPORTS -> "स्थानिक नागरिकांच्या प्रत्यक्ष अहवालांद्वारे पुष्टी केलेले मॉडेल मार्गदर्शन.";
                case OFFICIAL_CONFIRMED -> "सरकारी आपत्ती व्यवस्थापन प्राधिकरणाद्वारे अधिकृतपणे पुष्टी केलेले.";
            };
            case en -> switch (provenance) {
                case MODEL_ONLY -> "Guidance based on numerical model forecasts. Please verify locally before taking emergency action. Not an official government decree.";
                case MODEL_PLUS_REPORTS -> "Model guidance corroborated by local community ground observations.";
                case OFFICIAL_CONFIRMED -> "Officially confirmed by government disaster authorities.";
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

    private ComposedMessage composeEnglish(Hazard hazard, Severity severity, String locationName, String peakDate, String provLabel, Provenance provenance) {
        String provStatement = getProvenanceStatement(provenance, Language.en);
        String provNote;
        if (provenance == Provenance.MODEL_ONLY) {
            provNote = provLabel + " — Not an official warning. Confirm locally. " + provStatement;
        } else {
            provNote = provLabel + " — " + provStatement;
        }

        TemplateFields f = switch (hazard) {
            case FLOOD -> switch (severity) {
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
            case HEAT -> switch (severity) {
                case SEVERE -> new TemplateFields(
                        "🚨 KESTREL SEVERE HEAT WAVE EMERGENCY: " + locationName,
                        "Severe heat wave expected within the next 24 to 48 hours (Peak expected on " + peakDate + ").",
                        "Action: Severe heat wave expected. Stay indoors during hottest hours and seek medical help if someone feels dizzy or faint."
                );
                case WARNING -> new TemplateFields(
                        "⚠️ KESTREL HEAT WAVE WARNING: " + locationName,
                        "Heat wave conditions likely on consecutive days (Peak expected on " + peakDate + ").",
                        "Action: Heat wave conditions likely on consecutive days. Avoid going out between 12 noon and 3 PM; drink water frequently."
                );
                case WATCH -> new TemplateFields(
                        "👁️ KESTREL HEAT WATCH: " + locationName,
                        "Elevated temperatures and heat wave watch in effect within the next 72 hours (Expected on " + peakDate + ").",
                        "Action: Stay hydrated, monitor local temperature updates, and limit prolonged sun exposure."
                );
                case NONE -> new TemplateFields(
                        "✅ KESTREL HEAT ALL CLEAR: " + locationName,
                        "Temperatures have normalized within climatological limits. Normal conditions have resumed.",
                        "Action: Normal outdoor activities may resume. Stay hydrated and check regular forecasts."
                );
            };
            case AIR_QUALITY -> switch (severity) {
                case SEVERE -> new TemplateFields(
                        "🚨 KESTREL SEVERE AIR QUALITY EMERGENCY: " + locationName,
                        "Severe air quality emergency forecast within the next 24 to 48 hours (Peak expected on " + peakDate + ").",
                        "Action: Air quality is severe. Stay indoors as much as possible and seek immediate medical help for breathing difficulties."
                );
                case WARNING -> new TemplateFields(
                        "⚠️ KESTREL AIR QUALITY WARNING: " + locationName,
                        "Air quality index forecast in Very Poor category within the next 48 hours (Peak expected on " + peakDate + ").",
                        "Action: Air quality is very poor. Avoid outdoor exercise, keep windows closed, and wear an N95 mask if outdoors."
                );
                case WATCH -> new TemplateFields(
                        "👁️ KESTREL AIR QUALITY WATCH: " + locationName,
                        "Air quality index forecast in Poor category within the next 72 hours (Expected on " + peakDate + ").",
                        "Action: Sensitive individuals should reduce prolonged outdoor exertion and wear a mask in dusty areas."
                );
                case NONE -> new TemplateFields(
                        "✅ KESTREL AIR QUALITY ALL CLEAR: " + locationName,
                        "Air quality index has returned to acceptable levels. Normal conditions have resumed.",
                        "Action: Normal outdoor activities may resume."
                );
            };
        };

        String fullText = f.title() + "\n" + f.body() + "\n" + f.advice() + "\nProvenance: " + provNote;
        return new ComposedMessage(Language.en, severity, f.title(), f.body(), f.advice(), provNote, fullText);
    }

    private ComposedMessage composeHindi(Hazard hazard, Severity severity, String locationName, String peakDate, String provLabel, Provenance provenance) {
        String provStatement = getProvenanceStatement(provenance, Language.hi);
        String provNote;
        if (provenance == Provenance.MODEL_ONLY) {
            provNote = provLabel + " — यह आधिकारिक चेतावनी नहीं है। स्थानीय प्रशासन के निर्देशों का पालन करें। " + provStatement;
        } else {
            provNote = provLabel + " — " + provStatement;
        }

        TemplateFields f = switch (hazard) {
            case FLOOD -> switch (severity) {
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
            case HEAT -> switch (severity) {
                case SEVERE -> new TemplateFields(
                        "🚨 केस्ट्रल भीषण लू आपातकाल: " + locationName,
                        "अगले 24 से 48 घंटों में भीषण लू चलने की संभावना है (अधिकतम तापमान: " + peakDate + ")।",
                        "सुरक्षा निर्देश: भीषण लू की संभावना। सबसे गर्म घंटों में घर के अंदर रहें और चक्कर या बेहोशी महसूस होने पर तत्काल डॉक्टर से संपर्क करें।"
                );
                case WARNING -> new TemplateFields(
                        "⚠️ केस्ट्रल लू चेतावनी: " + locationName,
                        "अगले 48 घंटों में लू चलने की प्रबल संभावना है (अधिकतम तापमान: " + peakDate + ")।",
                        "सुरक्षा निर्देश: लगातार दिनों में लू चलने की संभावना है। दोपहर 12 बजे से 3 बजे के बीच धूप में निकलने से बचें; पर्याप्त पानी पिएं।"
                );
                case WATCH -> new TemplateFields(
                        "👁️ केस्ट्रल लू निगरानी: " + locationName,
                        "अगले 72 घंटों में तापमान में वृद्धि और लू की निगरानी जारी है (संभावित तिथि: " + peakDate + ")।",
                        "सुरक्षा निर्देश: मौसम के पूर्वानुमानों पर नज़र रखें, भरपूर पानी पिएं और धूप में अधिक समय तक रहने से बचें।"
                );
                case NONE -> new TemplateFields(
                        "✅ केस्ट्रल ताप स्थिति सामान्य: " + locationName,
                        "तापमान सामान्य जलवायु सीमा के भीतर आ गया है। स्थिति सामान्य है।",
                        "सुरक्षा निर्देश: सामान्य गतिविधियां पुनः प्रारंभ की जा सकती हैं। पर्याप्त पानी पीते रहें।"
                );
            };
            case AIR_QUALITY -> switch (severity) {
                case SEVERE -> new TemplateFields(
                        "🚨 केस्ट्रल गंभीर वायु गुणवत्ता आपातकाल: " + locationName,
                        "अगले 24 से 48 घंटों में वायु गुणवत्ता गंभीर स्तर पर पहुंचने का अनुमान है (अधिकतम प्रदूषण: " + peakDate + ")।",
                        "सुरक्षा निर्देश: वायु गुणवत्ता गंभीर स्तर पर है। जितना संभव हो घर के अंदर रहें और सांस लेने में कठिनाई होने पर तुरंत चिकित्सा सहायता लें।"
                );
                case WARNING -> new TemplateFields(
                        "⚠️ केस्ट्रल वायु गुणवत्ता चेतावनी: " + locationName,
                        "अगले 48 घंटों में वायु गुणवत्ता बहुत खराब श्रेणी में रहने का अनुमान है (अधिकतम प्रदूषण: " + peakDate + ")।",
                        "सुरक्षा निर्देश: वायु गुणवत्ता बहुत खराब है। बाहर व्यायाम करने से बचें, खिड़कियां बंद रखें और बाहर जाते समय N95 मास्क पहनें।"
                );
                case WATCH -> new TemplateFields(
                        "👁️ केस्ट्रल वायु गुणवत्ता निगरानी: " + locationName,
                        "अगले 72 घंटों में वायु गुणवत्ता खराब श्रेणी में रहने का अनुमान है (संभावित तिथि: " + peakDate + ")।",
                        "सुरक्षा निर्देश: संवेदनशील लोग बाहर अधिक परिश्रम से बचें और धूल वाले क्षेत्रों में मास्क पहनें।"
                );
                case NONE -> new TemplateFields(
                        "✅ केस्ट्रल वायु गुणवत्ता सामान्य: " + locationName,
                        "वायु गुणवत्ता स्वीकार्य स्तर पर आ गई है। स्थिति सामान्य है।",
                        "सुरक्षा निर्देश: सामान्य गतिविधियां पुनः प्रारंभ की जा सकती हैं।"
                );
            };
        };

        String fullText = f.title() + "\n" + f.body() + "\n" + f.advice() + "\nस्रोत: " + provNote;
        return new ComposedMessage(Language.hi, severity, f.title(), f.body(), f.advice(), provNote, fullText);
    }

    private ComposedMessage composeMarathi(Hazard hazard, Severity severity, String locationName, String peakDate, String provLabel, Provenance provenance) {
        String provStatement = getProvenanceStatement(provenance, Language.mr);
        String provNote;
        if (provenance == Provenance.MODEL_ONLY) {
            provNote = provLabel + " — हा अधिकृत इशारा नाही. स्थानिक पातळीवर खात्री करा. " + provStatement;
        } else {
            provNote = provLabel + " — " + provStatement;
        }

        TemplateFields f = switch (hazard) {
            case FLOOD -> switch (severity) {
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
            case HEAT -> switch (severity) {
                case SEVERE -> new TemplateFields(
                        "🚨 केस्ट्रल तीव्र उष्णतेची लाट आणीबाणी: " + locationName,
                        "पुढील २४ ते ४८ तासांत तीव्र उष्णतेची लाट येण्याची शक्यता आहे (अपेक्षित सर्वोच्च तापमान: " + peakDate + ").",
                        "सुरक्षा सूचना: तीव्र उष्णतेची लाट येण्याची शक्यता. दुपारच्या कडक उन्हात घरातच राहा आणि चक्कर किंवा अस्वस्थ वाटल्यास ताबडतोब वैद्यकीय मदत घ्या."
                );
                case WARNING -> new TemplateFields(
                        "⚠️ केस्ट्रल उष्णतेची लाट इशारा: " + locationName,
                        "पुढील दिवसांत उष्णतेची लाट येण्याची शक्यता आहे (अपेक्षित सर्वोच्च तापमान: " + peakDate + ").",
                        "सुरक्षा सूचना: सलग दिवस उष्णतेची लाट राहण्याची शक्यता आहे. दुपारी १२ ते ३ दरम्यान उन्हात जाणे टाळा; भरपूर पाणी प्या."
                );
                case WATCH -> new TemplateFields(
                        "👁️ केस्ट्रल उष्णता पूर्वसूचना: " + locationName,
                        "पुढील ७२ तासांत तापमान वाढण्याची शक्यता आहे (अपेक्षित तारीख: " + peakDate + ").",
                        "सुरक्षा सूचना: हवामानाच्या अंदाजावर लक्ष ठेवा, भरपूर पाणी प्या आणि जास्त वेळ उन्हात जाणे टाळा."
                );
                case NONE -> new TemplateFields(
                        "✅ केस्ट्रल तापमान पूर्ववत: " + locationName,
                        "तापमान सामान्य मर्यादेत आले आहे. परिस्थिती पूर्ववत झाली आहे.",
                        "सुरक्षा सूचना: दैनंदिन कामकाज पूर्ववत सुरू करता येईल. पुरेसे पाणी प्या."
                );
            };
            case AIR_QUALITY -> switch (severity) {
                case SEVERE -> new TemplateFields(
                        "🚨 केस्ट्रल अतिगंभीर हवा गुणवत्ता आणीबाणी: " + locationName,
                        "पुढील २४ ते ४८ तासांत हवेची गुणवत्ता 'गंभीर' श्रेणीत पोहोचण्याची शक्यता आहे (अपेक्षित सर्वोच्च पातळी: " + peakDate + ").",
                        "सुरक्षा सूचना: हवेची गुणवत्ता गंभीर पातळीवर आहे. शक्यतो घरातच राहा आणि श्वास घेण्यास त्रास झाल्यास त्वरित वैद्यकीय मदत घ्या."
                );
                case WARNING -> new TemplateFields(
                        "⚠️ केस्ट्रल हवा गुणवत्ता इशारा: " + locationName,
                        "पुढील ४८ तासांत हवेची गुणवत्ता 'अतिशय वाईट' श्रेणीत जाण्याची शक्यता आहे (अपेक्षित सर्वोच्च पातळी: " + peakDate + ").",
                        "सुरक्षा सूचना: हवेची गुणवत्ता खूप खालावली आहे. बाहेर व्यायाम करणे टाळा, खिडक्या बंद ठेवा आणि बाहेर पडताना N95 मास्क वापरा."
                );
                case WATCH -> new TemplateFields(
                        "👁️ केस्ट्रल हवा गुणवत्ता पूर्वसूचना: " + locationName,
                        "पुढील ७२ तासांत हवेची गुणवत्ता 'वाईट' श्रेणीत राहण्याची शक्यता आहे (अपेक्षित तारीख: " + peakDate + ").",
                        "सुरक्षा सूचना: संवेदनशील व्यक्तींनी बाहेर जास्त वेळ फिरणे टाळावे आणि धूळ असलेल्या भागात मास्क वापरावा."
                );
                case NONE -> new TemplateFields(
                        "✅ केस्ट्रल हवा गुणवत्ता पूर्ववत: " + locationName,
                        "हवेची गुणवत्ता समाधानकारक पातळीवर आली आहे. परिस्थिती पूर्ववत झाली आहे.",
                        "सुरक्षा सूचना: दैनंदिन बाहेरील कामे पूर्ववत सुरू करता येतील."
                );
            };
        };

        String fullText = f.title() + "\n" + f.body() + "\n" + f.advice() + "\nमाहिती स्रोत: " + provNote;
        return new ComposedMessage(Language.mr, severity, f.title(), f.body(), f.advice(), provNote, fullText);
    }
}
