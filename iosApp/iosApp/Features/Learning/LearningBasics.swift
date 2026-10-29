/** Presents the shared beginner curriculum with native accessible disclosure controls. */
import SwiftUI
import SharedLogic

struct LearningBasics: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Learn before you trade").font(.headline)
            ForEach(LearningCatalog.shared.lessons, id: \.id) { lesson in
                DisclosureGroup(lesson.title) { Text(lesson.explanation).font(.subheadline).padding(.vertical, 6) }
            }
        }
    }
}
