/** @type {import("jest").Config} */
module.exports = {
  preset: "ts-jest",
  testEnvironment: "node",
  testMatch: [
    "<rootDir>/test/**/*.test.ts"
  ],
  transform: {
    "^.+\\.tsx?$": [
      "ts-jest",
      {
        tsconfig: "tsconfig.test.json"
      }
    ]
  },
  clearMocks: true,
  restoreMocks: true,
  testTimeout: 60000
};