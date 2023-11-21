# frozen_string_literal: true

source 'https://rubygems.org'

git_source(:github) { |repo_name| "https://github.com/#{repo_name}" }

gem 'fastlane'
gem 'json'
gem 'rubocop', '1.38'
gem 'rubocop-performance'
gem 'rubocop-require_tools'
gem 'sinatra'

plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)
