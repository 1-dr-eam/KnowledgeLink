package com.github.forum.service.recommend;

import java.util.List;

public interface UserBehaviorProfileProvider {
    UserInterestProfile getUserInterestProfile(Long userId);

    class UserInterestProfile {
        private final List<String> categories;
        private final List<String> keywords;

        public UserInterestProfile(List<String> categories, List<String> keywords) {
            this.categories = categories;
            this.keywords = keywords;
        }

        public List<String> getCategories() {
            return categories;
        }

        public List<String> getKeywords() {
            return keywords;
        }
    }
}
