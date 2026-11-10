package com.keephub.core.data.daily

import java.time.LocalDate
import kotlin.math.abs

object DailyWordBank {

    val words: Map<VocabularyLevel, List<LeveledWord>> = mapOf(
        VocabularyLevel.BEGINNER to listOf(
            LeveledWord(
                term = "achieve",
                pos = "verb",
                ipa = "/əˈtʃiːv/",
                definition = "To successfully reach a goal or standard by effort, skill, or courage.",
                example = "With consistent daily practice, you will achieve your vocabulary goals.",
                mnemonic = "Think of reaching the top of an arch after a determined climb.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "ancient",
                pos = "adjective",
                ipa = "/ˈeɪnʃənt/",
                definition = "Belonging to the very distant past and no longer in existence.",
                example = "The archaeologists uncovered an ancient stone temple hidden in the forest.",
                mnemonic = "Sounds like 'age-ent'—belonging to a very distant age.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "brave",
                pos = "adjective",
                ipa = "/breɪv/",
                definition = "Ready to face and endure danger, pain, or difficulty; showing courage.",
                example = "The brave firefighter entered the smoke-filled building to rescue the family.",
                mnemonic = "Brave rhymes with save—brave people save others in danger.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "brilliant",
                pos = "adjective",
                ipa = "/ˈbrɪljənt/",
                definition = "Exceptionally clever, talented, or impressive; shining brightly.",
                example = "She came up with a brilliant solution that solved the puzzle in minutes.",
                mnemonic = "Like a brilliant diamond that radiates dazzling light.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "curious",
                pos = "adjective",
                ipa = "/ˈkjʊəriəs/",
                definition = "Eager to know, investigate, or learn something new.",
                example = "The curious child asked dozens of questions about how stars form in space.",
                mnemonic = "CURIous minds seek to CURE their lack of knowledge.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "diligent",
                pos = "adjective",
                ipa = "/ˈdɪlɪdʒənt/",
                definition = "Having or showing earnest care and steady effort in one's work.",
                example = "A diligent student reviews vocabulary notes every single day.",
                mnemonic = "DI-LI-GENT: Daily Intelligence Leads to Genuine Excellence.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "eager",
                pos = "adjective",
                ipa = "/ˈiːɡər/",
                definition = "Strongly wanting to do or have something; enthusiastically keen.",
                example = "The entire team was eager to launch the new software update.",
                mnemonic = "Picture an EAGLE swooping down with eager focus toward its goal.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "enormous",
                pos = "adjective",
                ipa = "/ɪˈnɔːməs/",
                definition = "Extremely large in size, quantity, scale, or extent.",
                example = "An enormous ancient oak tree shaded the entire front courtyard.",
                mnemonic = "E-NORMOUS: Exceeding the NORMAL size by far.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "explore",
                pos = "verb",
                ipa = "/ɪkˈsplɔːr/",
                definition = "To travel through an unfamiliar area in order to learn about it.",
                example = "We decided to explore the winding alleys of the historic quarter.",
                mnemonic = "EX-PLORE: Step outside to explore and see more.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "generous",
                pos = "adjective",
                ipa = "/ˈdʒenərəs/",
                definition = "Willing to give more of something, especially money or time, than is required.",
                example = "She was very generous with her time, mentoring younger students after work.",
                mnemonic = "GEN-erous comes from the same Latin root as genuine and gentle.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "gentle",
                pos = "adjective",
                ipa = "/ˈdʒentl/",
                definition = "Mild, kind, or tender in manner, behavior, or touch.",
                example = "He handled the injured sparrow with a remarkably gentle touch.",
                mnemonic = "Gentle people are gentlemanly and serene.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "grateful",
                pos = "adjective",
                ipa = "/ˈɡreɪtfl/",
                definition = "Feeling or showing appreciation for kindness or benefits received.",
                example = "I am deeply grateful for the encouragement my mentor offered.",
                mnemonic = "GRATE-ful: Feeling that someone's support was GREAT.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "hesitate",
                pos = "verb",
                ipa = "/ˈhezɪteɪt/",
                definition = "To pause before saying or doing something due to uncertainty or doubt.",
                example = "Do not hesitate to ask for clarification if anything seems unclear.",
                mnemonic = "HESITATE: H-Hold, E-Evaluate, S-Stop In The Act.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "honest",
                pos = "adjective",
                ipa = "/ˈɒnɪst/",
                definition = "Free of deceit and untruthfulness; sincere and candid.",
                example = "Giving honest, constructive feedback helps your peers grow quickly.",
                mnemonic = "Honest people earn honor and enduring trust.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "humble",
                pos = "adjective",
                ipa = "/ˈhʌmbl/",
                definition = "Having or showing a modest or low estimate of one's own importance.",
                example = "Despite winning the gold medal, the athlete remained humble and gracious.",
                mnemonic = "HUMBLE comes from Latin 'humus' (soil)—staying grounded to earth.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "innocent",
                pos = "adjective",
                ipa = "/ˈɪnəsnt/",
                definition = "Not guilty of a crime or offense; harmless and pure in intention.",
                example = "The court found the accused entirely innocent of all charges.",
                mnemonic = "In-nocent: 'noc' means harm (as in noxious)—literally 'not harmful'.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "loyal",
                pos = "adjective",
                ipa = "/ˈlɔɪəl/",
                definition = "Giving or showing firm and constant support or allegiance to a person or institution.",
                example = "Dogs are celebrated worldwide for being fiercely loyal companions.",
                mnemonic = "Loyal friends stay by your side through royal and difficult times.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "modern",
                pos = "adjective",
                ipa = "/ˈmɒdn/",
                definition = "Relating to the present or recent times as opposed to the remote past.",
                example = "The university library installed modern ergonomic desks and high-speed Wi-Fi.",
                mnemonic = "MOD-ern: Suited for the current MODe of living.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "notice",
                pos = "verb",
                ipa = "/ˈnəʊtɪs/",
                definition = "To become aware of something by seeing, hearing, or observing it.",
                example = "Did you notice the subtle improvement in your recall speed today?",
                mnemonic = "Make a mental NOTE whenever you notice something key.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "observe",
                pos = "verb",
                ipa = "/əbˈzɜːv/",
                definition = "To watch someone or something carefully in order to gain information.",
                example = "Biologists observe the seasonal migration patterns of arctic geese.",
                mnemonic = "OB-SERVE: Keep your senses serving you by watching closely.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "patient",
                pos = "adjective",
                ipa = "/ˈpeɪʃnt/",
                definition = "Able to accept or tolerate delays, problems, or suffering without becoming annoyed.",
                example = "Language mastery takes time, so be patient with your daily progress.",
                mnemonic = "A patient physician stays composed with every patient.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "polite",
                pos = "adjective",
                ipa = "/pəˈlaɪt/",
                definition = "Having or showing behavior that is respectful and considerate of other people.",
                example = "A polite greeting in the morning creates a warm atmosphere for everyone.",
                mnemonic = "Po-LITE: Shine a light of kindness upon others.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "precious",
                pos = "adjective",
                ipa = "/ˈpreʃəs/",
                definition = "Of great value; not to be wasted, squandered, or treated carelessly.",
                example = "Time spent reading quality books is precious for your intellectual growth.",
                mnemonic = "Precious like a priceless, cherished diamond.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "preserve",
                pos = "verb",
                ipa = "/prɪˈzɜːv/",
                definition = "To maintain something in its original, safe, or healthy state.",
                example = "The national park was founded to preserve endangered wildlife habitats.",
                mnemonic = "PRE-SERVE: Serve beforehand to protect and keep intact.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "proud",
                pos = "adjective",
                ipa = "/praʊd/",
                definition = "Feeling deep pleasure or satisfaction as a result of one's own or others' achievements.",
                example = "Her parents were proud of her consistent dedication to her exams.",
                mnemonic = "PR-OUT: Standing tall with confidence radiating outward.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "rare",
                pos = "adjective",
                ipa = "/reər/ ",
                definition = "Not occurring very often; seldom found and remarkably uncommon.",
                example = "Spotting a snow leopard in its natural Himalayan habitat is a rare event.",
                mnemonic = "Rare sightings are barely there.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "reveal",
                pos = "verb",
                ipa = "/rɪˈviːl/",
                definition = "To make previously unknown, secret, or hidden information visible to others.",
                example = "The final chapter will reveal who sent the mysterious anonymous letter.",
                mnemonic = "RE-VEIL: Take off the veil so the truth is uncovered.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "scarce",
                pos = "adjective",
                ipa = "/skeəs/",
                definition = "Insufficient for the demand; rare to find and in short supply.",
                example = "Fresh drinking water became scarce during the prolonged summer drought.",
                mnemonic = "SCAR-ce: Leaves a scar because resources are so thin.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "seldom",
                pos = "adverb",
                ipa = "/ˈseldəm/",
                definition = "Not often; rarely occurring.",
                example = "They seldom watch television, preferring to read novels in the evening.",
                mnemonic = "SEL-DOM: Selected seldom times.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "sincere",
                pos = "adjective",
                ipa = "/sɪnˈsɪər/",
                definition = "Proceeding from genuine feelings; honest, heartfelt, and unpretentious.",
                example = "He offered a sincere apology when he realized his words caused confusion.",
                mnemonic = "From Latin 'sine cera' (without wax)—pure marble without cosmetic filler.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "stubborn",
                pos = "adjective",
                ipa = "/ˈstʌbərn/",
                definition = "Having or showing dogged determination not to change one's attitude or opinion.",
                example = "He was too stubborn to ask for directions even when he knew he was lost.",
                mnemonic = "Stubbornly rooted in place like a solid tree stump.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "unique",
                pos = "adjective",
                ipa = "/juːˈniːk/",
                definition = "Being the only one of its kind; unlike anything else in comparison.",
                example = "Every human fingerprint features a completely unique swirl pattern.",
                mnemonic = "UNI-que: Uni means one—one of a kind.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "urgent",
                pos = "adjective",
                ipa = "/ˈɜːdʒənt/",
                definition = "Requiring immediate action, intervention, or focused attention.",
                example = "The hospital issued an urgent appeal for rare blood type donations.",
                mnemonic = "URG-ent: An intense urge that cannot wait.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "vast",
                pos = "adjective",
                ipa = "/vɑːst/",
                definition = "Of very great extent, area, or quantity; immense in size.",
                example = "The Pacific Ocean represents a vast expanse that spans across hemispheres.",
                mnemonic = "VAST: Very Astonishingly Spread Throughout.",
                level = VocabularyLevel.BEGINNER
            ),
            LeveledWord(
                term = "wisdom",
                pos = "noun",
                ipa = "/ˈwɪzdəm/",
                definition = "The quality of having experience, deep knowledge, and sound judgment.",
                example = "Elderly mentors offer invaluable wisdom on balancing career and well-being.",
                mnemonic = "WISE-dom: The accumulated domain of the wise.",
                level = VocabularyLevel.BEGINNER
            )
        ),
        VocabularyLevel.INTERMEDIATE to listOf(
            LeveledWord(
                term = "abundant",
                pos = "adjective",
                ipa = "/əˈbʌndənt/",
                definition = "Existing or available in large quantities; overflowing and plentiful.",
                example = "The tropical rainforest supports an abundant diversity of bird species.",
                mnemonic = "A-BUN-dant: Picture a bakery packed with an abundance of fresh buns.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "allocate",
                pos = "verb",
                ipa = "/ˈæləkeɪt/",
                definition = "To distribute resources or duties for a specific, predetermined purpose.",
                example = "The department voted to allocate more budget toward classroom technology.",
                mnemonic = "AL-LOCATE: Locate the resources and assign them to a goal.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "ambiguous",
                pos = "adjective",
                ipa = "/æmˈbɪɡjuəs/",
                definition = "Open to more than one interpretation; having a double or unclear meaning.",
                example = "The ambiguous wording in the contract triggered a dispute between both parties.",
                mnemonic = "AMBI = both (like ambidextrous)—pointing in two contradictory directions.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "articulate",
                pos = "adjective",
                ipa = "/ɑːˈtɪkjələt/",
                definition = "Having or showing the ability to speak fluently, clearly, and persuasively.",
                example = "She delivered an articulate keynote speech that captivated the auditorium.",
                mnemonic = "ART-iculate: Speaking with the refined clarity of an artist.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "coherent",
                pos = "adjective",
                ipa = "/kəʊˈhɪərənt/",
                definition = "Logical and consistent; parts forming a well-structured and unified whole.",
                example = "The research paper presented a coherent argument supported by empirical data.",
                mnemonic = "CO-HERE: Things that stick together in logical unity.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "comprehensive",
                pos = "adjective",
                ipa = "/ˌkɒmprɪˈhensɪv/",
                definition = "Including or dealing with all or nearly all elements or aspects of something.",
                example = "The university published a comprehensive syllabus covering every topic in the exam.",
                mnemonic = "COMPREHEND-sive: Enables full comprehension because it covers all angles.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "convey",
                pos = "verb",
                ipa = "/kənˈveɪ/",
                definition = "To communicate a message, idea, or impression; to transport.",
                example = "The poet chose simple words to convey profound emotional resonance.",
                mnemonic = "Like a conveyor belt transporting ideas directly into the listener's mind.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "deliberate",
                pos = "adjective",
                ipa = "/dɪˈlɪbərət/",
                definition = "Done consciously, intentionally, and after careful thought.",
                example = "He made a deliberate choice to study vocabulary for thirty minutes each morning.",
                mnemonic = "DE-LIBRA-te: Weighed on the scales (Libra) before making a decision.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "diminish",
                pos = "verb",
                ipa = "/dɪˈmɪnɪʃ/",
                definition = "To make or become less; to decrease in size, strength, or importance.",
                example = "Spaced repetition ensures your recall strength does not diminish over time.",
                mnemonic = "MIN-ish: Make smaller (like miniature or minus).",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "distinguish",
                pos = "verb",
                ipa = "/dɪˈstɪŋɡwɪʃ/",
                definition = "To recognize or treat someone or something as distinct and different.",
                example = "Advanced learners learn to distinguish between subtle shades of meaning.",
                mnemonic = "Find what is DISTINCT between the options.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "elaborate",
                pos = "verb",
                ipa = "/ɪˈlæbəreɪt/",
                definition = "To develop or present something in detail; to add more complexity.",
                example = "The professor asked the student to elaborate on the theoretical model.",
                mnemonic = "E-LABOR-ate: Invest extra labor to build out the full details.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "emphasize",
                pos = "verb",
                ipa = "/ˈemfəsaɪz/",
                definition = "To give special importance, stress, or prominence to something.",
                example = "The coach emphasized the importance of teamwork and consistency.",
                mnemonic = "EMPHASIS: Shine a spotlight on the central point.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "fluctuate",
                pos = "verb",
                ipa = "/ˈflʌktʃueɪt/",
                definition = "To rise and fall irregularly in number, level, or intensity.",
                example = "Currency exchange rates fluctuate constantly throughout the trading day.",
                mnemonic = "FLU-ctuate: Flows like a wave in water (flu = flow).",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "genuine",
                pos = "adjective",
                ipa = "/ˈdʒenjuɪn/",
                definition = "Truly what something is said to be; completely authentic and sincere.",
                example = "Her genuine enthusiasm for astronomy inspired everyone in the club.",
                mnemonic = "GENU-ine: Originating from authentic roots (gene).",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "hierarchy",
                pos = "noun",
                ipa = "/ˈhaɪərɑːki/",
                definition = "A system in which members or items are ranked according to status or priority.",
                example = "The corporate hierarchy defines clear reporting channels across departments.",
                mnemonic = "HIER-archy: Higher and lower tiers of organization.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "implicit",
                pos = "adjective",
                ipa = "/ɪmˈplɪsɪt/",
                definition = "Implied though not plainly or directly expressed.",
                example = "There was an implicit understanding that all project updates were confidential.",
                mnemonic = "IM-PLICIT: Folded inside (implied), in contrast to explicit.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "inevitable",
                pos = "adjective",
                ipa = "/ɪnˈevɪtəbl/",
                definition = "Certain to happen; unavoidable regardless of attempts to prevent it.",
                example = "Occasional mistakes are inevitable when learning a challenging new discipline.",
                mnemonic = "IN-EVITABLE: You cannot evade it.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "integrate",
                pos = "verb",
                ipa = "/ˈɪntɪɡreɪt/",
                definition = "To combine one thing with another so that they form a unified whole.",
                example = "The app allows students to integrate vocabulary review into their daily routine.",
                mnemonic = "INTEGER: A whole number—combine parts to make it whole.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "justify",
                pos = "verb",
                ipa = "/ˈdʒʌstɪfaɪ/",
                definition = "To show or prove to be right, reasonable, or warranted.",
                example = "He provided solid benchmark data to justify upgrading the company servers.",
                mnemonic = "JUST-ify: Prove that a decision is fair, correct, and JUST.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "legitimate",
                pos = "adjective",
                ipa = "/lɪˈdʒɪtɪmət/",
                definition = "Conforming to the law or rules; defensible, valid, and justifiable.",
                example = "The committee acknowledged that the student had a legitimate excuse for the delay.",
                mnemonic = "LEG-itimate: Grounded in legal rules and authentic standards.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "modify",
                pos = "verb",
                ipa = "/ˈmɒdɪfaɪ/",
                definition = "To make partial or minor changes to something to improve it.",
                example = "We will modify the quiz algorithm to adapt to each learner's difficulty.",
                mnemonic = "MOD-ify: Adjust the parameters to fit the current mode.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "narrative",
                pos = "noun",
                ipa = "/ˈnærətɪv/",
                definition = "A spoken or written account of connected events; an engaging story.",
                example = "The novel presents an unforgettable narrative about resilience and hope.",
                mnemonic = "NARRATE-ive: A story narrated with purposeful structure.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "notable",
                pos = "adjective",
                ipa = "/ˈnəʊtəbl/",
                definition = "Worthy of attention, praise, or notice; distinguished and remarkable.",
                example = "The team made notable progress in reducing latency across network requests.",
                mnemonic = "NOTE-able: Worth taking note of in your journal.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "obscure",
                pos = "adjective",
                ipa = "/əbˈskjʊər/",
                definition = "Not discovered or known about; difficult to understand or see clearly.",
                example = "The essay quoted an obscure eighteenth-century botanical manuscript.",
                mnemonic = "OBS-CURE: Shrouded in darkness so it is hard to see.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "persist",
                pos = "verb",
                ipa = "/pəˈsɪst/",
                definition = "To continue firmly in an action despite difficulty, opposition, or setbacks.",
                example = "If you persist through early obstacles, fluency will follow naturally.",
                mnemonic = "PER-SIST: Stand through (per = through, sist = stand).",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "plausible",
                pos = "adjective",
                ipa = "/ˈplɔːzəbl/",
                definition = "Seeming reasonable, credible, or probable in the given context.",
                example = "Her hypothesis seemed entirely plausible in light of recent findings.",
                mnemonic = "PLAUS-ible: Deserving applause because it makes logical sense.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "prevalent",
                pos = "adjective",
                ipa = "/ˈprevələnt/",
                definition = "Widespread in a particular area, community, or period of time.",
                example = "Digital note-taking has become increasingly prevalent on university campuses.",
                mnemonic = "PRE-VAIL-ent: Prevailed widely across the entire field.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "profound",
                pos = "adjective",
                ipa = "/prəˈfaʊnd/",
                definition = "Very great, intense, or insightful; extending deep beneath the surface.",
                example = "The philosophical essay had a profound influence on my thinking.",
                mnemonic = "PRO-FOUND: Reaching deep down to the foundational level.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "prosper",
                pos = "verb",
                ipa = "/ˈprɒspər/",
                definition = "To flourish, succeed, or thrive, especially financially or personally.",
                example = "Local businesses began to prosper after the high-speed rail line opened.",
                mnemonic = "PROS-PER: Move forward with positive momentum.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "reconcile",
                pos = "verb",
                ipa = "/ˈrekənsaɪl/ ",
                definition = "To restore friendly relations between; to make contradictory ideas compatible.",
                example = "The researchers sought to reconcile conflicting data from the two experiments.",
                mnemonic = "RE-CONCILE: Bring back into council and harmony.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "reinforce",
                pos = "verb",
                ipa = "/ˌriːɪnˈfɔːs/",
                definition = "To strengthen or support an object, habit, structure, or argument.",
                example = "Daily active recall sessions reinforce neural pathways for lasting memory.",
                mnemonic = "RE-IN-FORCE: Apply extra force again to make sturdy.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "resilient",
                pos = "adjective",
                ipa = "/rɪˈzɪliənt/",
                definition = "Able to withstand, recover from, or adjust easily to misfortune or change.",
                example = "She remained remarkably resilient, turning every setback into a learning lesson.",
                mnemonic = "Like a flexible spring bouncing back to shape after tension.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "spontaneous",
                pos = "adjective",
                ipa = "/spɒnˈteɪniəs/",
                definition = "Performed or occurring as a result of a sudden inner impulse or natural process.",
                example = "A spontaneous applause erupted across the hall when the results were announced.",
                mnemonic = "SPON-taneous: Acting on the spur of the moment naturally.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "tangible",
                pos = "adjective",
                ipa = "/ˈtændʒəbl/",
                definition = "Perceptible by touch; clear, definite, and concrete rather than abstract.",
                example = "Students experienced tangible improvements in retention within two weeks.",
                mnemonic = "From Latin 'tangere' (to touch)—something you can feel in reality.",
                level = VocabularyLevel.INTERMEDIATE
            ),
            LeveledWord(
                term = "validate",
                pos = "verb",
                ipa = "/ˈvælɪdeɪt/",
                definition = "To check or prove the validity, authenticity, or accuracy of something.",
                example = "Peer-reviewed studies are essential to validate new scientific discoveries.",
                mnemonic = "VALID-ate: Confirm that a claim is genuinely VALID.",
                level = VocabularyLevel.INTERMEDIATE
            )
        ),
        VocabularyLevel.ADVANCED to listOf(
            LeveledWord(
                term = "aberration",
                pos = "noun",
                ipa = "/ˌæbəˈreɪʃn/",
                definition = "A departure from what is normal, usual, or expected, typically an unwelcome one.",
                example = "The sudden plunge in server speed was treated as a temporary network aberration.",
                mnemonic = "AB-ERR-ation: Straying away from the path (err = to wander).",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "acquiesce",
                pos = "verb",
                ipa = "/ˌækwiˈes/",
                definition = "To accept something reluctantly but without protest; to comply silently.",
                example = "The director decided to acquiesce to the revised deadline proposed by the board.",
                mnemonic = "AC-QUIET-esce: Stay quiet and agree without making a scene.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "anomaly",
                pos = "noun",
                ipa = "/əˈnɒməli/",
                definition = "Something that deviates from what is standard, normal, or expected.",
                example = "Astronomers detected an unexplained magnetic anomaly in the outer solar system.",
                mnemonic = "A-NOMAL-y: A deviation that is NOT NORMAL.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "audacious",
                pos = "adjective",
                ipa = "/ɔːˈdeɪʃəs/",
                definition = "Showing a willingness to take surprisingly bold risks; daring and fearless.",
                example = "The team made an audacious bid to overhaul the entire architecture in one sprint.",
                mnemonic = "AUDA-cious: Having the audacity to take bold strides.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "austere",
                pos = "adjective",
                ipa = "/ɔːˈstɪər/",
                definition = "Severe or strict in manner or appearance; having no comforts or luxuries.",
                example = "The scholar lived an austere life dedicated entirely to historical research.",
                mnemonic = "AUS-TERE: Stern, severe, and stripped of excess.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "cacophony",
                pos = "noun",
                ipa = "/kəˈkɒfəni/",
                definition = "A harsh, discordant mixture of loud, jarring sounds.",
                example = "A cacophony of construction equipment and honking horns filled the morning air.",
                mnemonic = "CACO (bad, ugly) + PHONY (sound) = An unbearable, jarring racket.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "capricious",
                pos = "adjective",
                ipa = "/kəˈprɪʃəs/",
                definition = "Given to sudden and unaccountable changes of mood, decision, or behavior.",
                example = "The tyrant's capricious rulings kept his ministers in constant anxiety.",
                mnemonic = "CAPRI-cious: Like a capricious mountain goat leaping unpredictably.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "delineate",
                pos = "verb",
                ipa = "/dɪˈlɪnieɪt/",
                definition = "To describe or portray something with precision; to indicate exact boundaries.",
                example = "The agreement clearly delineates the intellectual property rights of each cofounder.",
                mnemonic = "DE-LINE-ate: Draw the lines clearly so boundaries are evident.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "deprecation",
                pos = "noun",
                ipa = "/ˌdeprɪˈkeɪʃn/",
                definition = "Expression of strong disapproval; declaring a feature or practice obsolete.",
                example = "The release notes announced the gradual deprecation of the older REST endpoints.",
                mnemonic = "DE-PRAY-cate: Warning that a legacy feature is being retired.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "deride",
                pos = "verb",
                ipa = "/dɪˈraɪd/",
                definition = "To express contempt for; to ridicule and mock mockingly.",
                example = "Traditional critics derided the impressionist paintings when they first debuted.",
                mnemonic = "DE-RIDE: Ride down with mockery (Latin 'ridere' = to laugh at).",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "didactic",
                pos = "adjective",
                ipa = "/daɪˈdæktɪk/",
                definition = "Intended to teach, particularly having moral instruction as an ulterior motive.",
                example = "The fable was charming, though its ending felt slightly too didactic.",
                mnemonic = "DID-ACT-ic: Designed to tell you how you should act.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "disparate",
                pos = "adjective",
                ipa = "/ˈdɪspərət/",
                definition = "Essentially different in kind; not allowing practical comparison.",
                example = "The researcher synthesized ideas from disparate domains such as music and neuroscience.",
                mnemonic = "DIS-PAIR-ate: You cannot pair them up because they are too distinct.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "ephemeral",
                pos = "adjective",
                ipa = "/ɪˈfemərəl/",
                definition = "Lasting for a very short, fleeting period of time; transitory.",
                example = "The beauty of cherry blossoms in April is wondrous yet deeply ephemeral.",
                mnemonic = "E-PHEMERAL: Like an ephemeral email or fleeting breeze.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "equivocal",
                pos = "adjective",
                ipa = "/ɪˈkwɪvəkl/",
                definition = "Open to more than one interpretation; deliberately ambiguous and misleading.",
                example = "The diplomat provided an equivocal answer that avoided taking a firm stance.",
                mnemonic = "EQUI-VOCAL: Equal voices saying contradictory things.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "esoteric",
                pos = "adjective",
                ipa = "/ˌesəˈterɪk/",
                definition = "Intended for or likely to be understood by only a small number of specialists.",
                example = "His lecture delved into esoteric mathematical principles of string theory.",
                mnemonic = "ESO-TERIC: Understood only by an inner circle of initiates.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "fastidious",
                pos = "adjective",
                ipa = "/fæˈstɪdiəs/",
                definition = "Very attentive to and concerned about accuracy, detail, and cleanliness.",
                example = "The head chef was fastidious about the presentation of every single dish.",
                mnemonic = "FAST-TIDY-ous: Needs everything tidy and precise at all times.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "gregarious",
                pos = "adjective",
                ipa = "/ɡrɪˈɡeəriəs/",
                definition = "Fond of company; sociable, outgoing, and living in flocks or groups.",
                example = "Her gregarious personality made her popular at networking conferences.",
                mnemonic = "From Latin 'grex' (flock)—loves being part of the group.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "innocuous",
                pos = "adjective",
                ipa = "/ɪˈnɒkjuəs/",
                definition = "Not harmful or offensive; innocuous in effect and harmless.",
                example = "What seemed like an innocuous question provoked a surprisingly intense debate.",
                mnemonic = "IN-NOCUOUS: In (not) + noc (harmful, like noxious) = harmless.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "intractable",
                pos = "adjective",
                ipa = "/ɪnˈtræktəbl/",
                definition = "Hard to control, manage, or deal with; stubborn and resistant to solutions.",
                example = "Inflation proved an intractable problem that baffled seasoned central bankers.",
                mnemonic = "IN-TRACTABLE: Cannot be tractored or pulled along easily.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "loquacious",
                pos = "adjective",
                ipa = "/ləˈkweɪʃəs/",
                definition = "Tending to talk a great deal; extremely talkative and chatty.",
                example = "The loquacious tour guide shared endless fascinating anecdotes about the palace.",
                mnemonic = "LOQ-uacious: Root 'loq' means speak (like eloquent and dialogue).",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "meticulous",
                pos = "adjective",
                ipa = "/məˈtɪkjələs/",
                definition = "Showing great attention to detail; very careful, precise, and scrupulous.",
                example = "The restorer conducted meticulous cleaning on the centuries-old oil canvas.",
                mnemonic = "MET-ICULOUS: Every detail met with perfection.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "obfuscate",
                pos = "verb",
                ipa = "/ˈɒbfʌskeɪt/",
                definition = "To render obscure, unclear, or unintelligible; to deliberately confuse.",
                example = "Politicians sometimes use technical jargon to obfuscate the real issues.",
                mnemonic = "OB-FUSCATE: Put into a dark fog or confused blur.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "ostentatious",
                pos = "adjective",
                ipa = "/ˌɒstenˈteɪʃəs/",
                definition = "Characterized by pretentious, conspicuous show in order to impress others.",
                example = "He wore an ostentatious diamond-studded watch to the casual charity dinner.",
                mnemonic = "OSTENTATION: Always putting on an extravagant, flashy show.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "parsimonious",
                pos = "adjective",
                ipa = "/ˌpɑːsɪˈməʊniəs/",
                definition = "Unwilling to spend money or use resources; excessively frugal or stingy.",
                example = "The parsimonious company founder refused to hire assistant editors.",
                mnemonic = "PARSI-MONEY-ous: Parts with his money with immense reluctance.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "pervasive",
                pos = "adjective",
                ipa = "/pəˈveɪsɪv/",
                definition = "Spreading widely throughout an area or a group of people; inescapable.",
                example = "Artificial intelligence has become a pervasive influence across modern industries.",
                mnemonic = "PER-VADE: Pervading throughout every layer of society.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "pragmatic",
                pos = "adjective",
                ipa = "/præɡˈmætɪk/",
                definition = "Dealing with things sensibly and realistically based on practical considerations.",
                example = "She favored a pragmatic engineering design over a purely aesthetic one.",
                mnemonic = "PRAG-matic: Practical people seek practical outcomes.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "repudiate",
                pos = "verb",
                ipa = "/rɪˈpjuːdieɪt/",
                definition = "To refuse to accept or be associated with; to firmly deny the truth of.",
                example = "The ministry issued a statement to repudiate the unfounded allegations.",
                mnemonic = "RE-PUD-iate: Put away and refuse association firmly.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "reticent",
                pos = "adjective",
                ipa = "/ˈretɪsnt/",
                definition = "Not revealing one's thoughts or feelings readily; reserved and silent.",
                example = "He remained reticent about his past accomplishments, preferring humility.",
                mnemonic = "RETI-CENT: Reluctant to share sentiments openly.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "salubrious",
                pos = "adjective",
                ipa = "/səˈluːbriəs/",
                definition = "Health-giving; healthy and wholesome in environment or climate.",
                example = "The cool mountain climate and clean pine air provided a salubrious retreat.",
                mnemonic = "SALU-brious: Like Spanish 'salud' meaning health.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "sycophant",
                pos = "noun",
                ipa = "/ˈsɪkəfænt/",
                definition = "A person who acts obsequiously toward someone in power in order to gain an advantage.",
                example = "The tyrannical ruler surrounded himself with sycophants who never dared dissent.",
                mnemonic = "PSYCHO-FAN: A fawning flatterer eager to please.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "taciturn",
                pos = "adjective",
                ipa = "/ˈtæsɪtɜːn/",
                definition = "Reserved or uncommunicative in speech; saying very little by nature.",
                example = "The sea captain was a taciturn man whose silence commanded quiet respect.",
                mnemonic = "TACIT-urn: Keeping tacit (unspoken) and quiet.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "ubiquitous",
                pos = "adjective",
                ipa = "/juːˈbɪkwɪtəs/",
                definition = "Present, appearing, or found everywhere at the same time.",
                example = "Smartphones have become so ubiquitous that life without them seems distant.",
                mnemonic = "U-BI-QUIT-ous: You can't quit seeing it everywhere you look.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "vacillate",
                pos = "verb",
                ipa = "/ˈvæsɪleɪt/",
                definition = "To alternate or waver between different opinions or actions; to hesitate indecisively.",
                example = "She vacillated between accepting the promotion and pursuing graduate school.",
                mnemonic = "VACIL-LATE: Oscillating like a pendulum, arriving late to a decision.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "venerate",
                pos = "verb",
                ipa = "/ˈvenəreɪt/",
                definition = "To regard with great respect, admiration, and deep reverence.",
                example = "In many Asian cultures, younger generations venerate the wisdom of their elders.",
                mnemonic = "VENER-ate: Revere like a venerable, honored sage.",
                level = VocabularyLevel.ADVANCED
            ),
            LeveledWord(
                term = "zealous",
                pos = "adjective",
                ipa = "/ˈzeləs/",
                definition = "Having or showing intense zeal, dedication, passion, and enthusiasm.",
                example = "The zealous volunteers spent their entire weekend planting trees in the park.",
                mnemonic = "ZEAL-ous: Filled with vibrant zeal and tireless drive.",
                level = VocabularyLevel.ADVANCED
            )
        )
    )

    fun getWordsForDate(
        level: VocabularyLevel,
        date: LocalDate,
        existingTerms: Set<String> = emptySet()
    ): List<LeveledWord> {
        val pool = words[level] ?: words[VocabularyLevel.INTERMEDIATE]!!
        val epochDay = date.toEpochDay()
        val startIndex = ((epochDay % pool.size + pool.size) % pool.size).toInt()

        val result = mutableListOf<LeveledWord>()
        var offset = 0
        // Prioritize words that aren't already saved in user's vocabulary
        while (result.size < 5 && offset < pool.size) {
            val candidate = pool[(startIndex + offset) % pool.size]
            if (candidate.term.lowercase() !in existingTerms) {
                result.add(candidate)
            }
            offset++
        }
        // If user already saved almost all words, backfill to guarantee 5 words
        offset = 0
        while (result.size < 5 && offset < pool.size) {
            val candidate = pool[(startIndex + offset) % pool.size]
            if (candidate !in result) {
                result.add(candidate)
            }
            offset++
        }
        return result.take(5)
    }
}
